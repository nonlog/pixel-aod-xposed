# ADR 0018: Centralize contextual target arbitration

Date: 2026-08-22
Status: Accepted

## Context

The current contextual selector handles module-owned Weather Alert, Calendar, and Forecast sources. Android 17 parity work adds read-only native Smartspace-style targets and read-only Live Updates, while existing media and notification presentation already compete for limited low-power screen area. If each adapter decides visibility independently, equivalent targets can duplicate, stale content can linger, and presentation density can exceed the accepted low-power visual budget.

## Decision

Introduce one **contextual target arbiter** before contextual content reaches the COUI scene owner.

1. Normalize native contextual targets, Live Updates, module Weather/Forecast/Alert/Calendar sources, and future contextual sources into a common arbitration input.
2. Rank deterministically using urgency, validity/TTL, selected-user/work-profile privacy scope, ambient suppression, and presentation eligibility.
3. Deduplicate native and module-owned targets that represent the same real-world item or semantic content.
4. Enforce the low-power visual budget before committing contextual output to the scene.
5. Keep the final visible contextual set small and deterministic; adapters provide data and metadata but do not independently claim screen rows.
6. Preserve explicit module fallbacks when an equivalent native target is absent, invalid, stale, or privacy-suppressed.

## Consequences

- Contextual presentation has one testable priority and deduplication policy.
- Smartspace and Live Update integration cannot silently multiply AOD rows.
- Privacy, suppression, and visual-budget policy can be applied consistently across source types.

## Rejected alternatives

- Stack every eligible source until the screen is full: violates low-power density goals and produces unstable layouts.
- Let each adapter decide visibility independently: creates competing owners, duplicate content, and difficult-to-test priority behavior.

## M9 P1-S18 implementation note — 2026-08-23

- The common model is now `ContextualTarget`; the single policy owner is `ContextualTargetArbiter`.
- Existing module Weather Alert, Calendar and Forecast inputs are the first producers. They retain the prior visible priority while sharing one eligibility/deduplication/budget path.
- Deduplication intentionally follows eligibility so an invalid/suppressed native equivalent cannot remove a valid module fallback.
- Weather Alert visibility history is committed only after the alert wins arbitration and is actually visible.
- The S14 typed `contextualPresentation` slot is consumed only as its own capability; current-ROM `UNKNOWN` remains fail-open.
- Native Smartspace, Live Update and Ambient Indication source kinds are model seams only in S18. No native target is created until a stable current-OOS source surface is proven.
