# ADR 0046: Enforce a five-percent module-owned AOD OPR release gate

Date: 2026-08-22
Status: Accepted

## Context

ADR 0006 already requires a measurable low-power visual budget for Pixel-owned AOD scenes, but it intentionally deferred the numerical threshold. AOSP guidance recommends keeping ambient-display on-pixel ratio around five percent, and the project now has enough defined scenes to make that recommendation an enforceable release criterion.

## Decision

Adopt a **five-percent module AOD OPR release gate**.

1. Representative fully-dozed Pixel-owned scenes must measure at or below 5% on-pixel ratio.
2. Required coverage includes large, compact, media, contextual, notification-overflow, and both ADR 0041 AOD palette modes.
3. Prefer deterministic screenshot or off-screen rendering measurement in automated validation.
4. Exclude vendor-owned UDFPS/biometric surfaces and native pulse/promoted foreground content from the module-owned pixel budget.
5. When a deterministic scene exceeds the budget, change layout/presentation policy rather than randomly hiding runtime content.
6. Burn-in movement remains a separate mandatory requirement and does not substitute for passing OPR.

## Consequences

- ADR 0006 becomes a concrete release gate instead of a qualitative design goal.
- Rich-content additions require measurable evidence before release.
- Palette and layout changes can be regression-tested against a stable power-oriented metric.

## Rejected alternatives

- Warning at 5% and failing only at a higher threshold: weakens the accepted AOSP-oriented target.
- Log OPR without a failure threshold: provides diagnostics but no enforceable safety contract.
