# ADR 0044: Force fully-dozed presentation styling for AOD-off notification pulses

Date: 2026-08-22
Status: Accepted

## Context

Android 17 can enter a notification pulse/Dozing scene while continuous always-on display is disabled. In that case SystemUI presents the clock as fully dozed rather than leaving lockscreen styling behind the pulse. Pixel AOD's general continuous Doze transition architecture does not by itself define this special presentation override.

## Decision

Add a **pulse doze-style override**.

1. When continuous native AOD is disabled but OPlus/SystemUI authorizes a notification pulse/valid Dozing scene, classify the Pixel background as fully dozed immediately for presentation purposes.
2. Apply the selected ADR 0041 AOD palette plus fully-dozed clock weight, alpha, and geometry semantics during that pulse window.
3. Do not interpret the override as continuous AOD enablement and do not start/extend a module-owned Doze lifecycle.
4. End the override when the vendor pulse/Dozing scene ends, a terminal gate fires, or native scene eligibility no longer permits it.
5. Continue to respect ADR 0021 native pulse foreground ownership and ADR 0038 collision geometry.

## Consequences

- AOD-off notification pulses no longer show an incorrect lockscreen-styled background.
- Pulse presentation remains visually consistent without granting the module power/lifecycle ownership.
- Q16 continuous transition progress remains valid for ordinary LS/AOD handoffs while this explicit pulse case has deterministic semantics.

## Rejected alternatives

- Animate from lockscreen styling for every AOD-off pulse: introduces an incorrect intermediate state after SystemUI already classifies the scene as Dozing.
- Keep lockscreen styling throughout the pulse: diverges from Android Doze presentation.
