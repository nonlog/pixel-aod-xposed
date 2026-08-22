# ADR 0057: Prefer native AOD notification icon ordering

Date: 2026-08-22
Status: Accepted

## Context

ADR 0024 decides which notifications are eligible for Pixel AOD, and ADR 0048 decides how many eligible icons fit and how overflow is represented. The current renderer still inherits ordering from the module notification snapshot, which is not guaranteed to match the final SystemUI ambient ordering and can produce unstable visual order across rebuilds.

## Decision

Add a read-only **native AOD notification icon order adapter**.

1. Prefer the final ordered ambient notification keys supplied by a stable OPlus/SystemUI seam.
2. Apply ADR 0024 eligibility first, then native ordering, then ADR 0048 capacity/overflow.
3. Do not invent a module importance/post-time/package ranking when native ordering is available.
4. If no reliable native ordering seam exists, use a deterministic stable fallback that preserves order for unchanged inputs.
5. Reordering alone may refresh icon presentation, but it must not change pulse eligibility or notification lifetime.

## Consequences

- Notification eligibility, ordering, capacity, and visual metrics become separate testable responsibilities.
- Pixel icon order can match native ambient semantics instead of listener delivery order.
- Rebuilds no longer need to reshuffle unchanged notifications.

## Rejected alternatives

- Preserve raw listener snapshot order: that order is not an authoritative AOD ranking contract.
- Create a module ranking formula: duplicates SystemUI policy and risks divergence from vendor behavior.
