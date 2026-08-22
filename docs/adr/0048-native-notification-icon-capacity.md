# ADR 0048: Separate native notification icon capacity from eligibility

Date: 2026-08-22
Status: Accepted

## Context

ADR 0024 makes finalized native AOD notification eligibility the preferred source for deciding which notifications may appear. The current renderer separately hard-codes `MAX_NOTIFICATION_ICONS = 5` and renders `+N` overflow text. Android/SystemUI has its own AOD/lockscreen icon-capacity policy and overflow-dot semantics, so capacity should not be embedded in module eligibility or a universal constant.

## Decision

Add a read-only **native notification icon capacity adapter** and use native/AOSP-style overflow semantics.

1. Keep notification eligibility under ADR 0024; this adapter decides only how many eligible icons fit in the current AOD/lockscreen row.
2. Prefer stable OPlus/SystemUI AOD and lockscreen icon-capacity values when available.
3. Render overflow using native/AOSP-style overflow-dot semantics instead of permanent `+N` text.
4. When no reliable native capacity exists, derive a deterministic safe capacity from ADR 0045 target-region geometry, icon size, spacing, RTL direction, and current configuration.
5. Recompute capacity when target region, density/font scale, scene, or layout direction changes.
6. Keep the final row within ADR 0046 OPR and ADR 0006 visual-budget constraints.

## Consequences

- Eligibility, capacity, and visual overflow become independently testable responsibilities.
- The hard-coded five-icon assumption can be removed during M9 implementation.
- Notification rows adapt safely to device/configuration geometry without copying native visual containers.

## Rejected alternatives

- Keep five icons plus `+N` on all layouts: encodes a module-specific behavior unrelated to native capacity.
- Make capacity dynamic but retain `+N`: improves geometry but leaves the AOD overflow visual semantic divergent.
