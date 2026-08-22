# ADR 0035: Obey vendor terminal Doze gates without reassertion

Date: 2026-08-22
Status: Accepted

## Context

Android 17 `DozeSuppressor` can end Doze immediately for terminal conditions such as an unprovisioned/unset-up current user or pending authentication. Pixel AOD's older keepalive/reassert paths can rewrite or resist vendor hide/OFF behavior. Under ADR 0001, a vendor decision to finish Doze must be stronger than any module presentation preference.

## Decision

Add a read-only **vendor Doze terminal gate**.

1. Consume reliable OPlus/SystemUI terminal conditions including current-user setup/provisioning failure and pending-authentication states that require Doze to end.
2. On a terminal gate, immediately remove Pixel presentation and release stock suppression.
3. Cancel or bypass every module reassert, native-hide suppression, or `OFF -> DOZE` rewrite while the terminal condition applies.
4. Do not treat terminal gates as ordinary transient visibility suppression.
5. Re-enter Pixel presentation only after a new valid vendor Doze/AOD lifecycle begins and normal policy permits it.

## Consequences

- Pixel AOD cannot fight a legitimate SystemUI Doze FINISH/OFF transition.
- Existing keepalive technical debt has a clear removal boundary for M9.
- Authentication and provisioning transitions avoid stale or resurrected AOD overlays.

## Rejected alternatives

- Handle provisioning only: leaves pending-auth terminal semantics vulnerable to reassertion.
- Rely solely on eventual screen state: permits races where the module can briefly restore presentation after SystemUI has decided to terminate Doze.
