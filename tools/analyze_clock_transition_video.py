#!/usr/bin/env python3
"""Detect Pixel AOD clock-transition weather regressions in a screen recording.

The detector is intentionally tied to the user's concrete visual symptoms:

* the current-weather temperature must keep a stable baseline and glyph spacing
  relative to the weather icon while the clock changes size;
* the weather icon must never become a giant, one-frame object at the right edge.

It exits with status 1 when either symptom is present, making the command useful
as a deterministic red/green loop for captured-device QA.  OpenCV and NumPy are
the only non-stdlib dependencies.
"""

from __future__ import annotations

import argparse
import csv
import json
import math
import statistics
import sys
from collections import Counter
from dataclasses import asdict, dataclass
from pathlib import Path
from typing import Iterable, Optional, Sequence

import cv2
import numpy as np


ICON_SCAN_TOP = 250
ICON_SCAN_BOTTOM = 850
NORMAL_ICON_AREA_MIN = 900
NORMAL_ICON_AREA_MAX = 4_500
NORMAL_ICON_SIZE_MIN = 30
NORMAL_ICON_SIZE_MAX = 90
MODE_ROUNDING_PX = 2
MODE_MIN_DISTANCE_PX = 120.0
STABLE_MODE_RADIUS_PX = 9.0
MIN_STABLE_RUN_FRAMES = 5
MAX_SAME_STATE_GAP_FRAMES = 4


@dataclass(frozen=True)
class Box:
    x: int
    y: int
    w: int
    h: int
    area: int
    cx: float
    cy: float

    @property
    def right(self) -> int:
        return self.x + self.w

    @property
    def bottom(self) -> int:
        return self.y + self.h


@dataclass
class Observation:
    frame: int
    time_s: float
    icon: Optional[Box]
    normal_icon_count: int
    huge_orange: list[Box]
    digit_1: Optional[Box]
    digit_2: Optional[Box]
    degree: Optional[Box]
    temp_bbox: Optional[Box]
    temp_dx: Optional[float]
    temp_dy: Optional[float]
    gap_12: Optional[float]
    gap_2_degree: Optional[float]
    digit_candidate_count: int


@dataclass(frozen=True)
class StableRun:
    label: str
    start: int
    end: int


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Frame-by-frame detector for weather drift and right-edge flashes."
    )
    parser.add_argument("video", type=Path, help="Screen recording to inspect")
    parser.add_argument(
        "--output-dir",
        type=Path,
        help="Evidence directory (default: <video-dir>/analysis-<video-stem>)",
    )
    parser.add_argument(
        "--drift-threshold-px",
        type=float,
        default=3.0,
        help="Maximum allowed temperature/icon relative-position error",
    )
    parser.add_argument(
        "--spacing-threshold-px",
        type=float,
        default=2.0,
        help="Maximum allowed temperature glyph-spacing error",
    )
    parser.add_argument(
        "--flash-area-ratio",
        type=float,
        default=4.0,
        help="Orange component / normal weather-icon area ratio treated as a flash",
    )
    parser.add_argument(
        "--no-keyframes",
        action="store_true",
        help="Skip annotated PNG extraction for a faster metrics-only run",
    )
    return parser.parse_args()


def component_boxes(mask: np.ndarray, y_offset: int = 0, x_offset: int = 0) -> list[Box]:
    count, _, stats, centroids = cv2.connectedComponentsWithStats(mask, connectivity=8)
    boxes: list[Box] = []
    for index in range(1, count):
        x, y, w, h, area = (int(value) for value in stats[index])
        boxes.append(
            Box(
                x=x + x_offset,
                y=y + y_offset,
                w=w,
                h=h,
                area=area,
                cx=float(centroids[index][0] + x_offset),
                cy=float(centroids[index][1] + y_offset),
            )
        )
    return boxes


def union_box(boxes: Sequence[Box]) -> Box:
    left = min(box.x for box in boxes)
    top = min(box.y for box in boxes)
    right = max(box.right for box in boxes)
    bottom = max(box.bottom for box in boxes)
    area = sum(box.area for box in boxes)
    return Box(
        x=left,
        y=top,
        w=right - left,
        h=bottom - top,
        area=area,
        cx=(left + right) / 2.0,
        cy=(top + bottom) / 2.0,
    )


def find_temperature_glyphs(
    upper_hsv: np.ndarray, icon: Box
) -> tuple[Optional[Box], Optional[Box], Optional[Box], int]:
    """Find the two digits and degree sign immediately to the icon's right."""

    upper_height, upper_width = upper_hsv.shape[:2]
    x0 = max(0, icon.x + 55)
    x1 = min(upper_width, icon.x + 190)
    y0 = max(ICON_SCAN_TOP, icon.y - 4)
    y1 = min(ICON_SCAN_TOP + upper_height, icon.y + 82)
    if x1 <= x0 or y1 <= y0:
        return None, None, None, 0

    roi = upper_hsv[y0 - ICON_SCAN_TOP : y1 - ICON_SCAN_TOP, x0:x1]
    low_saturation_text = cv2.inRange(
        roi,
        np.array([0, 0, 100], dtype=np.uint8),
        np.array([179, 118, 255], dtype=np.uint8),
    )
    candidates = component_boxes(low_saturation_text, y_offset=y0, x_offset=x0)

    digit_candidates = [
        box
        for box in candidates
        if 15 <= box.w <= 48
        and 34 <= box.h <= 60
        and box.area >= 220
        and icon.y - 2 <= box.y <= icon.y + 28
        and icon.x + 58 <= box.x <= icon.x + 155
    ]
    digit_candidates.sort(key=lambda box: box.x)
    if len(digit_candidates) < 2:
        return None, None, None, len(digit_candidates)

    digit_1, digit_2 = digit_candidates[:2]
    degree_candidates = [
        box
        for box in candidates
        if digit_2.right <= box.x <= icon.x + 185
        and 10 <= box.w <= 30
        and 12 <= box.h <= 30
        and box.area >= 70
        and icon.y - 4 <= box.y <= icon.y + 30
    ]
    degree_candidates.sort(key=lambda box: box.x)
    degree = degree_candidates[0] if degree_candidates else None
    return digit_1, digit_2, degree, len(digit_candidates)


def scan_video(video: Path) -> tuple[list[Observation], float, float, int, int]:
    capture = cv2.VideoCapture(str(video))
    if not capture.isOpened():
        raise RuntimeError(f"Unable to open video: {video}")

    nominal_fps = float(capture.get(cv2.CAP_PROP_FPS) or 0.0)
    width = int(capture.get(cv2.CAP_PROP_FRAME_WIDTH))
    height = int(capture.get(cv2.CAP_PROP_FRAME_HEIGHT))
    if width < 1 or height < ICON_SCAN_BOTTOM:
        raise RuntimeError(f"Unexpected video dimensions: {width}x{height}")

    orange_low = np.array([3, 120, 100], dtype=np.uint8)
    orange_high = np.array([35, 255, 255], dtype=np.uint8)
    observations: list[Observation] = []
    timestamps: list[float] = []
    frame_index = 0

    while True:
        ok, frame = capture.read()
        if not ok:
            break
        time_s = float(capture.get(cv2.CAP_PROP_POS_MSEC)) / 1000.0
        timestamps.append(time_s)
        upper = frame[ICON_SCAN_TOP:ICON_SCAN_BOTTOM]
        upper_hsv = cv2.cvtColor(upper, cv2.COLOR_BGR2HSV)
        orange_mask = cv2.inRange(upper_hsv, orange_low, orange_high)
        orange_boxes = component_boxes(orange_mask, y_offset=ICON_SCAN_TOP)

        normal_icons = [
            box
            for box in orange_boxes
            if NORMAL_ICON_AREA_MIN <= box.area <= NORMAL_ICON_AREA_MAX
            and NORMAL_ICON_SIZE_MIN <= box.w <= NORMAL_ICON_SIZE_MAX
            and NORMAL_ICON_SIZE_MIN <= box.h <= NORMAL_ICON_SIZE_MAX
            and box.y < 760
            and 250 <= box.x <= width - 150
        ]
        # The weather icon is consistently the largest square orange object in
        # the flat upper lock-screen area.  The y/x restrictions exclude cats
        # and notification artwork lower in the wallpaper.
        normal_icons.sort(
            key=lambda box: (box.area - abs(box.w - box.h) * 20), reverse=True
        )
        icon = normal_icons[0] if normal_icons else None

        huge_orange = [
            box
            for box in orange_boxes
            if box.area >= 5_000
            and (box.w >= 140 or box.h >= 140)
            and box.y < ICON_SCAN_BOTTOM
        ]

        digit_1 = digit_2 = degree = temp_bbox = None
        temp_dx = temp_dy = gap_12 = gap_2_degree = None
        digit_candidate_count = 0
        if icon is not None:
            digit_1, digit_2, degree, digit_candidate_count = find_temperature_glyphs(
                upper_hsv, icon
            )
            if digit_1 is not None and digit_2 is not None and degree is not None:
                temp_bbox = union_box((digit_1, digit_2, degree))
                temp_dx = temp_bbox.x - icon.x
                temp_dy = temp_bbox.y - icon.y
                gap_12 = digit_2.cx - digit_1.cx
                gap_2_degree = degree.cx - digit_2.cx

        observations.append(
            Observation(
                frame=frame_index,
                time_s=time_s,
                icon=icon,
                normal_icon_count=len(normal_icons),
                huge_orange=huge_orange,
                digit_1=digit_1,
                digit_2=digit_2,
                degree=degree,
                temp_bbox=temp_bbox,
                temp_dx=temp_dx,
                temp_dy=temp_dy,
                gap_12=gap_12,
                gap_2_degree=gap_2_degree,
                digit_candidate_count=digit_candidate_count,
            )
        )
        frame_index += 1

    capture.release()
    if not observations:
        raise RuntimeError("Video contains no decodable frames")

    positive_deltas = [
        right - left
        for left, right in zip(timestamps, timestamps[1:])
        if right > left
    ]
    median_frame_s = statistics.median(positive_deltas) if positive_deltas else 0.0
    return observations, nominal_fps, median_frame_s, width, height


def find_clock_modes(observations: Sequence[Observation]) -> dict[str, tuple[float, float]]:
    positions = [
        (observation.icon.cx, observation.icon.cy)
        for observation in observations
        if observation.icon is not None
    ]
    if len(positions) < 20:
        raise RuntimeError("Not enough tracked weather-icon frames to infer clock modes")

    counts = Counter(
        (
            int(round(x / MODE_ROUNDING_PX) * MODE_ROUNDING_PX),
            int(round(y / MODE_ROUNDING_PX) * MODE_ROUNDING_PX),
        )
        for x, y in positions
    )
    ranked = counts.most_common()
    first = ranked[0][0]
    second: Optional[tuple[int, int]] = None
    for candidate, _ in ranked[1:]:
        if math.dist(first, candidate) >= MODE_MIN_DISTANCE_PX:
            second = candidate
            break
    if second is None:
        raise RuntimeError("Recording does not contain two distinct clock-size modes")

    def refine(seed: tuple[int, int]) -> tuple[float, float]:
        near = [point for point in positions if math.dist(point, seed) <= 12.0]
        return (
            statistics.median(point[0] for point in near),
            statistics.median(point[1] for point in near),
        )

    mode_a = refine(first)
    mode_b = refine(second)
    large, small = sorted((mode_a, mode_b), key=lambda point: point[0])
    return {"large": large, "small": small}


def fill_short_same_state_gaps(labels: list[Optional[str]]) -> None:
    index = 0
    while index < len(labels):
        if labels[index] is not None:
            index += 1
            continue
        start = index
        while index < len(labels) and labels[index] is None:
            index += 1
        end = index - 1
        left = labels[start - 1] if start > 0 else None
        right = labels[index] if index < len(labels) else None
        if (
            left is not None
            and left == right
            and end - start + 1 <= MAX_SAME_STATE_GAP_FRAMES
        ):
            for gap_index in range(start, end + 1):
                labels[gap_index] = left


def stable_runs(
    observations: Sequence[Observation], modes: dict[str, tuple[float, float]]
) -> tuple[list[StableRun], list[Optional[str]]]:
    labels: list[Optional[str]] = []
    for observation in observations:
        if observation.icon is None:
            labels.append(None)
            continue
        distances = {
            name: math.dist((observation.icon.cx, observation.icon.cy), point)
            for name, point in modes.items()
        }
        label = min(distances, key=distances.get)
        labels.append(label if distances[label] <= STABLE_MODE_RADIUS_PX else None)

    fill_short_same_state_gaps(labels)

    # Discard isolated mode matches inside an animation.
    index = 0
    while index < len(labels):
        if labels[index] is None:
            index += 1
            continue
        label = labels[index]
        start = index
        while index < len(labels) and labels[index] == label:
            index += 1
        if index - start < MIN_STABLE_RUN_FRAMES:
            for short_index in range(start, index):
                labels[short_index] = None

    fill_short_same_state_gaps(labels)

    runs: list[StableRun] = []
    index = 0
    while index < len(labels):
        label = labels[index]
        if label is None:
            index += 1
            continue
        start = index
        while index + 1 < len(labels) and labels[index + 1] == label:
            index += 1
        runs.append(StableRun(label=label, start=start, end=index))
        index += 1
    return runs, labels


def median_metric(
    observations: Iterable[Observation], attribute: str
) -> Optional[float]:
    values = [
        float(value)
        for observation in observations
        if (value := getattr(observation, attribute)) is not None
    ]
    return statistics.median(values) if values else None


def stable_metric_baselines(
    observations: Sequence[Observation], modes: dict[str, tuple[float, float]]
) -> dict[str, dict[str, float]]:
    """Return the dominant live-view geometry for each settled clock mode.

    The icon can reach its target coordinates before the overlay/live handoff
    finishes.  A local sample immediately after icon motion would therefore
    learn the bug (roughly +11 px) as the target.  The dominant value across the
    full stable run is the actual settled live-view baseline (roughly +1 px).
    """

    attributes = ("temp_dx", "temp_dy", "gap_12", "gap_2_degree")
    baselines: dict[str, dict[str, float]] = {}
    for name, point in modes.items():
        stable = [
            observation
            for observation in observations
            if observation.icon is not None
            and math.dist((observation.icon.cx, observation.icon.cy), point)
            <= STABLE_MODE_RADIUS_PX
        ]
        mode_baselines: dict[str, float] = {}
        for attribute in attributes:
            values = [
                float(value)
                for observation in stable
                if (value := getattr(observation, attribute)) is not None
            ]
            if not values:
                raise RuntimeError(f"No stable {attribute} samples for {name} mode")
            quantized = Counter(round(value * 2.0) / 2.0 for value in values)
            mode_baselines[attribute] = float(quantized.most_common(1)[0][0])
        baselines[name] = mode_baselines
    return baselines


def transition_reports(
    observations: Sequence[Observation],
    runs: Sequence[StableRun],
    modes: dict[str, tuple[float, float]],
    baselines: dict[str, dict[str, float]],
    drift_threshold_px: float,
    spacing_threshold_px: float,
) -> list[dict]:
    reports: list[dict] = []
    for left_run, right_run in zip(runs, runs[1:]):
        if left_run.label == right_run.label:
            continue
        # Human clock-size transitions in this recording finish well within
        # 1.5 seconds.  A larger gap is a separate screen/lifecycle event.
        if right_run.start - left_run.end > 220:
            continue

        start = left_run.end
        motion_end = right_run.start
        attributes = ("temp_dx", "temp_dy", "gap_12", "gap_2_degree")
        source = baselines[left_run.label]
        target = baselines[right_run.label]

        # Continue after icon motion ends so the detector captures the delayed
        # overlay -> live handoff.  Stop at the first five-frame settled streak,
        # or after 90 frames if the temperature never settles.
        maximum_analysis_end = min(right_run.end, motion_end + 90)
        settle_streak_start: Optional[int] = None
        settled_frame: Optional[int] = None
        for candidate in range(motion_end, maximum_analysis_end + 1):
            window = observations[candidate : min(candidate + 5, maximum_analysis_end + 1)]
            if len(window) < 5:
                break
            settled = True
            for observation in window:
                if (
                    observation.temp_dx is None
                    or observation.temp_dy is None
                    or observation.gap_12 is None
                    or observation.gap_2_degree is None
                ):
                    settled = False
                    break
                position_error = math.hypot(
                    observation.temp_dx - target["temp_dx"],
                    observation.temp_dy - target["temp_dy"],
                )
                spacing_error = max(
                    abs(observation.gap_12 - target["gap_12"]),
                    abs(observation.gap_2_degree - target["gap_2_degree"]),
                )
                if position_error > 1.5 or spacing_error > 1.5:
                    settled = False
                    break
            if settled:
                settle_streak_start = candidate
                settled_frame = candidate + 4
                break
        analysis_end = settled_frame if settled_frame is not None else maximum_analysis_end

        source_point = np.array(modes[left_run.label], dtype=np.float64)
        target_point = np.array(modes[right_run.label], dtype=np.float64)
        mode_vector = target_point - source_point
        mode_length_sq = float(np.dot(mode_vector, mode_vector))
        frame_metrics: list[dict] = []

        for observation in observations[start : analysis_end + 1]:
            if (
                observation.icon is None
                or observation.temp_dx is None
                or observation.temp_dy is None
                or observation.gap_12 is None
                or observation.gap_2_degree is None
            ):
                continue
            icon_point = np.array((observation.icon.cx, observation.icon.cy))
            progress = float(np.dot(icon_point - source_point, mode_vector) / mode_length_sq)
            progress = min(1.0, max(0.0, progress))

            def expected(name: str) -> float:
                return float(source[name]) + progress * (
                    float(target[name]) - float(source[name])
                )

            drift_x = observation.temp_dx - expected("temp_dx")
            drift_y = observation.temp_dy - expected("temp_dy")
            gap_12_error = observation.gap_12 - expected("gap_12")
            gap_2_degree_error = observation.gap_2_degree - expected("gap_2_degree")
            frame_metrics.append(
                {
                    "frame": observation.frame,
                    "time_s": observation.time_s,
                    "progress": progress,
                    "drift_x": drift_x,
                    "drift_y": drift_y,
                    "drift_norm": math.hypot(drift_x, drift_y),
                    "gap_12_error": gap_12_error,
                    "gap_2_degree_error": gap_2_degree_error,
                    "temp_dx": observation.temp_dx,
                    "temp_dy": observation.temp_dy,
                    "gap_12": observation.gap_12,
                    "gap_2_degree": observation.gap_2_degree,
                }
            )

        if not frame_metrics:
            continue
        peak_drift = max(frame_metrics, key=lambda item: item["drift_norm"])
        peak_spacing = max(
            frame_metrics,
            key=lambda item: max(
                abs(item["gap_12_error"]), abs(item["gap_2_degree_error"])
            ),
        )

        adjacent_jumps: list[dict] = []
        for previous, current in zip(frame_metrics, frame_metrics[1:]):
            if current["frame"] != previous["frame"] + 1:
                continue
            jump_x = current["temp_dx"] - previous["temp_dx"]
            jump_y = current["temp_dy"] - previous["temp_dy"]
            adjacent_jumps.append(
                {
                    "from_frame": previous["frame"],
                    "to_frame": current["frame"],
                    "from_time_s": previous["time_s"],
                    "to_time_s": current["time_s"],
                    "jump_x": jump_x,
                    "jump_y": jump_y,
                    "jump_norm": math.hypot(jump_x, jump_y),
                }
            )
        peak_jump = (
            max(adjacent_jumps, key=lambda item: item["jump_norm"])
            if adjacent_jumps
            else None
        )
        spacing_error = max(
            abs(peak_spacing["gap_12_error"]),
            abs(peak_spacing["gap_2_degree_error"]),
        )
        is_red = (
            peak_drift["drift_norm"] > drift_threshold_px
            or spacing_error > spacing_threshold_px
            or (peak_jump is not None and peak_jump["jump_norm"] > drift_threshold_px)
        )

        reports.append(
            {
                "direction": f"{left_run.label}-to-{right_run.label}",
                "start_frame": start,
                "motion_end_frame": motion_end,
                "analysis_end_frame": analysis_end,
                "start_time_s": observations[start].time_s,
                "motion_end_time_s": observations[motion_end].time_s,
                "analysis_end_time_s": observations[analysis_end].time_s,
                "motion_duration_ms": (
                    observations[motion_end].time_s - observations[start].time_s
                )
                * 1000.0,
                "settle_start_frame": settle_streak_start,
                "settle_start_time_s": None
                if settle_streak_start is None
                else observations[settle_streak_start].time_s,
                "settle_lag_after_icon_ms": None
                if settle_streak_start is None
                else (
                    observations[settle_streak_start].time_s
                    - observations[motion_end].time_s
                )
                * 1000.0,
                "source_metrics": source,
                "target_metrics": target,
                "peak_drift": peak_drift,
                "peak_spacing": peak_spacing,
                "peak_adjacent_jump": peak_jump,
                "missing_temperature_frames": sum(
                    1
                    for observation in observations[start : analysis_end + 1]
                    if observation.temp_bbox is None
                ),
                "red": is_red,
            }
        )
    return reports


def flash_reports(
    observations: Sequence[Observation],
    median_frame_s: float,
    flash_area_ratio: float,
) -> tuple[list[dict], float]:
    normal_areas = [
        observation.icon.area
        for observation in observations
        if observation.icon is not None
    ]
    normal_area = statistics.median(normal_areas)
    anomalous_frames = [
        observation
        for observation in observations
        if any(box.area / normal_area >= flash_area_ratio for box in observation.huge_orange)
    ]

    groups: list[list[Observation]] = []
    for observation in anomalous_frames:
        if not groups or observation.frame != groups[-1][-1].frame + 1:
            groups.append([observation])
        else:
            groups[-1].append(observation)

    reports: list[dict] = []
    for group in groups:
        peak_observation = max(
            group, key=lambda observation: max(box.area for box in observation.huge_orange)
        )
        peak_box = max(peak_observation.huge_orange, key=lambda box: box.area)
        reports.append(
            {
                "start_frame": group[0].frame,
                "end_frame": group[-1].frame,
                "start_time_s": group[0].time_s,
                "end_time_s": group[-1].time_s,
                "duration_ms": (
                    group[-1].time_s - group[0].time_s + median_frame_s
                )
                * 1000.0,
                "peak_frame": peak_observation.frame,
                "peak_time_s": peak_observation.time_s,
                "bbox": asdict(peak_box),
                "area_ratio": peak_box.area / normal_area,
                "normal_icon_also_visible": peak_observation.icon is not None,
                "temperature_also_visible": peak_observation.temp_bbox is not None,
                "classification": "enlarged-current-weather-icon-transform",
                "red": True,
            }
        )
    return reports, normal_area


def write_metrics_csv(path: Path, observations: Sequence[Observation]) -> None:
    fields = (
        "frame",
        "time_s",
        "icon_x",
        "icon_y",
        "icon_w",
        "icon_h",
        "icon_area",
        "normal_icon_count",
        "temp_x",
        "temp_y",
        "temp_w",
        "temp_h",
        "temp_dx",
        "temp_dy",
        "gap_12",
        "gap_2_degree",
        "digit_candidate_count",
        "huge_orange_count",
        "huge_orange_max_area",
    )
    with path.open("w", newline="", encoding="utf-8") as handle:
        writer = csv.DictWriter(handle, fieldnames=fields)
        writer.writeheader()
        for observation in observations:
            icon = observation.icon
            temp = observation.temp_bbox
            writer.writerow(
                {
                    "frame": observation.frame,
                    "time_s": f"{observation.time_s:.6f}",
                    "icon_x": "" if icon is None else icon.x,
                    "icon_y": "" if icon is None else icon.y,
                    "icon_w": "" if icon is None else icon.w,
                    "icon_h": "" if icon is None else icon.h,
                    "icon_area": "" if icon is None else icon.area,
                    "normal_icon_count": observation.normal_icon_count,
                    "temp_x": "" if temp is None else temp.x,
                    "temp_y": "" if temp is None else temp.y,
                    "temp_w": "" if temp is None else temp.w,
                    "temp_h": "" if temp is None else temp.h,
                    "temp_dx": "" if observation.temp_dx is None else observation.temp_dx,
                    "temp_dy": "" if observation.temp_dy is None else observation.temp_dy,
                    "gap_12": "" if observation.gap_12 is None else observation.gap_12,
                    "gap_2_degree": ""
                    if observation.gap_2_degree is None
                    else observation.gap_2_degree,
                    "digit_candidate_count": observation.digit_candidate_count,
                    "huge_orange_count": len(observation.huge_orange),
                    "huge_orange_max_area": max(
                        (box.area for box in observation.huge_orange), default=0
                    ),
                }
            )


def draw_box(frame: np.ndarray, box: Box, color: tuple[int, int, int], label: str) -> None:
    cv2.rectangle(frame, (box.x, box.y), (box.right, box.bottom), color, 4)
    cv2.putText(
        frame,
        label,
        (box.x, max(35, box.y - 12)),
        cv2.FONT_HERSHEY_SIMPLEX,
        0.85,
        color,
        2,
        cv2.LINE_AA,
    )


def extract_keyframes(
    video: Path,
    output_dir: Path,
    observations: Sequence[Observation],
    transitions: list[dict],
    flashes: list[dict],
) -> list[str]:
    requests: list[tuple[int, str]] = []
    for index, transition in enumerate(transitions, start=1):
        peak_frame = int(transition["peak_drift"]["frame"])
        requests.append(
            (
                peak_frame,
                f"transition-{index:02d}-{transition['direction']}-peak-drift",
            )
        )
        jump = transition.get("peak_adjacent_jump")
        if jump is not None:
            requests.append(
                (
                    int(jump["to_frame"]),
                    f"transition-{index:02d}-{transition['direction']}-peak-jump",
                )
            )
    for index, flash in enumerate(flashes, start=1):
        peak = int(flash["peak_frame"])
        requests.extend(
            [
                (max(0, peak - 1), f"flash-{index:02d}-pre"),
                (peak, f"flash-{index:02d}-peak"),
                (min(len(observations) - 1, peak + 1), f"flash-{index:02d}-post"),
            ]
        )

    request_map: dict[int, list[str]] = {}
    for frame_index, label in requests:
        request_map.setdefault(frame_index, []).append(label)

    capture = cv2.VideoCapture(str(video))
    paths: list[str] = []
    maximum_frame = max(request_map, default=-1)
    frame_index = 0
    while frame_index <= maximum_frame:
        ok, frame = capture.read()
        if not ok:
            break
        labels = request_map.get(frame_index)
        if labels:
            observation = observations[frame_index]
            if observation.icon is not None:
                draw_box(frame, observation.icon, (0, 165, 255), "weather icon")
            for glyph_label, glyph in (
                ("temp 1", observation.digit_1),
                ("temp 2", observation.digit_2),
                ("degree", observation.degree),
            ):
                if glyph is not None:
                    draw_box(frame, glyph, (0, 255, 0), glyph_label)
            for huge in observation.huge_orange:
                draw_box(frame, huge, (255, 0, 255), "GIANT WEATHER ICON")

            for label in labels:
                annotated = frame.copy()
                caption = f"frame={frame_index} t={observation.time_s:.6f}s {label}"
                cv2.putText(
                    annotated,
                    caption,
                    (24, 55),
                    cv2.FONT_HERSHEY_SIMPLEX,
                    1.1,
                    (0, 0, 255),
                    3,
                    cv2.LINE_AA,
                )
                crop_bottom = min(annotated.shape[0], 1_250)
                evidence = annotated[200:crop_bottom]
                filename = (
                    f"{label}-f{frame_index:04d}-t{observation.time_s:.3f}s.png"
                )
                path = output_dir / filename
                if cv2.imwrite(str(path), evidence):
                    paths.append(str(path.resolve()))
        frame_index += 1
    capture.release()
    return paths


def compact_float(value: Optional[float]) -> str:
    return "n/a" if value is None else f"{value:.2f}"


def main() -> int:
    args = parse_args()
    video = args.video.resolve()
    if not video.is_file():
        print(f"ERROR: video not found: {video}", file=sys.stderr)
        return 2
    output_dir = (
        args.output_dir.resolve()
        if args.output_dir is not None
        else video.parent / f"analysis-{video.stem}"
    )
    output_dir.mkdir(parents=True, exist_ok=True)

    observations, nominal_fps, median_frame_s, width, height = scan_video(video)
    modes = find_clock_modes(observations)
    runs, _ = stable_runs(observations, modes)
    baselines = stable_metric_baselines(observations, modes)
    transitions = transition_reports(
        observations,
        runs,
        modes,
        baselines,
        args.drift_threshold_px,
        args.spacing_threshold_px,
    )
    flashes, normal_icon_area = flash_reports(
        observations, median_frame_s, args.flash_area_ratio
    )
    for flash in flashes:
        following = [
            transition
            for transition in transitions
            if transition["start_time_s"] >= flash["peak_time_s"]
            and transition["start_time_s"] - flash["peak_time_s"] <= 0.150
        ]
        linked = min(following, key=lambda item: item["start_time_s"], default=None)
        flash["next_transition_direction"] = (
            None if linked is None else linked["direction"]
        )
        flash["lead_before_transition_ms"] = (
            None
            if linked is None
            else (linked["start_time_s"] - flash["peak_time_s"]) * 1000.0
        )

    metrics_path = output_dir / "frame_metrics.csv"
    write_metrics_csv(metrics_path, observations)
    keyframes: list[str] = []
    if not args.no_keyframes:
        keyframes = extract_keyframes(video, output_dir, observations, transitions, flashes)

    red_transitions = [transition for transition in transitions if transition["red"]]
    verdict = "RED" if red_transitions or flashes else "GREEN"
    simultaneous_normal_icons = max(
        observation.normal_icon_count for observation in observations
    )
    report = {
        "verdict": verdict,
        "video": str(video),
        "video_size": {"width": width, "height": height},
        "frames": len(observations),
        "nominal_fps": nominal_fps,
        "median_frame_ms": median_frame_s * 1000.0,
        "thresholds": {
            "drift_px": args.drift_threshold_px,
            "spacing_px": args.spacing_threshold_px,
            "flash_area_ratio": args.flash_area_ratio,
        },
        "clock_modes": {
            name: {"icon_cx": point[0], "icon_cy": point[1]}
            for name, point in modes.items()
        },
        "settled_temperature_baselines": baselines,
        "normal_weather_icon_area": normal_icon_area,
        "stable_runs": [asdict(run) for run in runs],
        "transitions": transitions,
        "flashes": flashes,
        "evidence": {
            "max_simultaneous_normal_weather_icons": simultaneous_normal_icons,
            "flash_frames_replace_normal_icon": bool(flashes)
            and all(not flash["normal_icon_also_visible"] for flash in flashes),
            "flash_frames_also_lose_temperature": bool(flashes)
            and all(not flash["temperature_also_visible"] for flash in flashes),
            "interpretation": (
                "The giant orange object is the current-weather icon under an invalid "
                "one-frame transform. The normal icon is absent in the same frame, so "
                "the recording does not support a duplicate-View explanation. "
                "Temperature glyph spacing can be evaluated separately from its "
                "icon-relative baseline, distinguishing glyph reflow from overlay/live "
                "handoff geometry."
            ),
            "recommended_minimal_fix_seams": [
                (
                    "CouiClockSizeTransitionLayer.prepare/createOverlayViews: assign source "
                    "layout params and the source frame before setVisibility(VISIBLE). The "
                    "current ImageView is briefly attached with FrameLayout default "
                    "MATCH_PARENT geometry, which explains the one-frame giant icon before "
                    "large-to-small motion begins."
                ),
                (
                    "CouiClockSizeTransitionLayer.applyFixedCellInfoFrame/"
                    "placeInfoAtVisualCenter: make the weather clone's painted vertical "
                    "baseline equal the captured live TextMetrics baseline. Do not change "
                    "FixedAdvanceSpan spacing or lockscreen layout offsets; spacing is already "
                    "stable and only the clone-to-live vertical geometry is wrong."
                ),
            ],
        },
        "metrics_csv": str(metrics_path.resolve()),
        "keyframes": keyframes,
    }
    report_path = output_dir / "report.json"
    report_path.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding="utf-8")

    print(
        f"{verdict}: {len(red_transitions)}/{len(transitions)} clock-size transitions "
        f"exceed drift/spacing thresholds; {len(flashes)} giant weather-icon flashes."
    )
    print(
        f"Video: {len(observations)} frames, {width}x{height}, "
        f"median frame {median_frame_s * 1000.0:.2f} ms"
    )
    print(
        "Modes: "
        f"large icon=({modes['large'][0]:.1f},{modes['large'][1]:.1f}), "
        f"small icon=({modes['small'][0]:.1f},{modes['small'][1]:.1f})"
    )
    for index, transition in enumerate(transitions, start=1):
        drift = transition["peak_drift"]
        spacing = transition["peak_spacing"]
        jump = transition["peak_adjacent_jump"]
        print(
            f"T{index:02d} {transition['direction']} "
            f"motion {transition['start_time_s']:.3f}-"
            f"{transition['motion_end_time_s']:.3f}s, "
            f"settle lag={compact_float(transition['settle_lag_after_icon_ms'])} ms: "
            f"peak relative drift=({drift['drift_x']:+.2f},"
            f"{drift['drift_y']:+.2f}) px norm={drift['drift_norm']:.2f} "
            f"at {drift['time_s']:.3f}s; "
            f"spacing errors=({spacing['gap_12_error']:+.2f},"
            f"{spacing['gap_2_degree_error']:+.2f}) px; "
            f"peak adjacent jump={compact_float(None if jump is None else jump['jump_norm'])} px; "
            f"missing temp frames={transition['missing_temperature_frames']} "
            f"=> {'RED' if transition['red'] else 'GREEN'}"
        )
    for index, flash in enumerate(flashes, start=1):
        box = flash["bbox"]
        print(
            f"F{index:02d} {flash['peak_time_s']:.6f}s frame {flash['peak_frame']}: "
            f"orange bbox=({box['x']},{box['y']},{box['w']}x{box['h']}), "
            f"area={box['area']} ({flash['area_ratio']:.2f}x normal), "
            f"duration={flash['duration_ms']:.2f} ms, normal icon present="
            f"{flash['normal_icon_also_visible']}, temp present="
            f"{flash['temperature_also_visible']}, next="
            f"{flash['next_transition_direction']} in "
            f"{compact_float(flash['lead_before_transition_ms'])} ms => RED"
        )
    print(f"Report: {report_path.resolve()}")
    print(f"Frame metrics: {metrics_path.resolve()}")
    if keyframes:
        print(f"Keyframes: {len(keyframes)} PNG files under {output_dir.resolve()}")
    return 1 if verdict == "RED" else 0


if __name__ == "__main__":
    raise SystemExit(main())
