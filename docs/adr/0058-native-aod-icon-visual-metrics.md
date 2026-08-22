# ADR 0058: Consume native AOD notification icon visual metrics

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD currently renders notification glyphs using module-fixed dimensions and effectively full presentation alpha. Android/SystemUI defines AOD-specific notification icon opacity and geometry separately from ordinary lockscreen icon presentation. ADR 0048 already separates capacity from eligibility, but visual alpha, size, spacing, and overflow-dot geometry still need an owner.

## Decision

Add a read-only **native AOD icon visual-metrics adapter**.

1. Prefer stable OPlus/SystemUI Doze notification icon alpha, icon size, spacing, and overflow-dot geometry for the current AOD/lockscreen scene.
2. Keep ADR 0024 eligibility, ADR 0057 ordering, ADR 0048 capacity/overflow count, and ADR 0041 palette choice logically separate from these visual metrics.
3. When reliable vendor metrics are unavailable, use verified Android 17/AOSP AOD values as the fallback baseline, including the native Doze small-icon alpha behavior.
4. Clamp dimensions to ADR 0045 target-region geometry and include representative rows in ADR 0046 OPR testing.
5. Configuration, layout-direction, and scene changes recompute the metrics without changing notification semantics.

## Consequences

- Pixel notification glyph density and opacity can match native ambient presentation more closely.
- Fixed full-opacity AOD icon rendering becomes implementation debt rather than product policy.
- Icon semantics and icon visual treatment remain independently testable.

## Rejected alternatives

- Keep full-opacity fixed-DP icons on every build: diverges from native Doze presentation.
- Change alpha only while keeping all geometry fixed: fixes one metric but leaves the same device/configuration mismatch.
