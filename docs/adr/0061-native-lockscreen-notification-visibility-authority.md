# ADR 0061: Native lockscreen notification authority with a narrow OOS compatibility correction

Date: 2026-08-22
Status: Accepted (refined during M9 regression validation)

## Context

OPlus/SystemUI must remain the general authority for Keyguard notification privacy, profile scope, ranking, importance, and lockscreen visibility. However, the module already carried a targeted compatibility fix for a reproducible OOS defect: after notifications are visible on the lockscreen, unlocking once and then turning the screen off again can cause OOS to report those otherwise-eligible notifications as hidden and remove the entire lockscreen notification set.

During the first M9 implementation of Q61, the compatibility hook was reduced to a read-only observer. The user immediately reported the original OOS defect returning. That regression establishes that completely forbidding a corrective `hidden=true -> false` result is not equivalent to preserving native policy on this device; it also re-exposes a known OEM state bug.

## Decision

Adopt **native lockscreen notification visibility authority with a narrow OOS compatibility exception**.

1. OPlus/SystemUI owns the normal Keyguard notification visibility policy. The module does not implement a parallel general-purpose policy and does not proactively hide notifications that SystemUI would otherwise show.
2. Retain the user-facing `OOS lockscreen notification compatibility fix`. It may correct a native `hidden=true` result back to visible only for the known unlock-then-screen-off OOS regression and only when the notification is otherwise eligible for lockscreen presentation.
3. The correction predicate must remain deliberately narrow. It must not revive notifications that are missing a small icon, `VISIBILITY_SECRET`, ranking-secret, media/transport, SystemUI/Android-owned, module-internal non-test notifications, or known low-importance notifications. Native privacy/profile/DND and other accepted eligibility gates remain authoritative.
4. The compatibility path never changes a native visible result to hidden. The older module behavior that forced silent/low-importance notifications hidden is not restored.
5. The correction is explicitly user-controllable through the retained compatibility setting. Disabling it returns the hooks to observation-only behavior.
6. Pixel AOD/Lockscreen consumes the final corrected visibility result downstream for notification eligibility and layout. This exception does not grant the module ownership of notification ranking, ordering, pulse lifecycle, or general Keyguard privacy policy.

## Consequences

- The confirmed OOS unlock/relock notification-loss regression remains fixed.
- SystemUI remains the owner of the broad notification policy surface; only a tightly filtered OEM bug is corrected.
- Privacy-sensitive, media, system, and low-importance notifications are not revived by the compatibility path.
- M9 tests must cover both sides of the boundary: eligible ordinary notifications may be corrected from hidden to visible, while excluded categories remain hidden and visible native results are never force-hidden.

## Rejected alternatives

- Make both Keyguard visibility hooks permanently read-only: this was implemented in early M9 and reproduced the known OOS regression, so it is not acceptable on the validated device.
- Restore the old broad `Lockscreen Policy` behavior including module-owned low-importance hiding: that creates a competing notification policy owner and exceeds the scope of the compatibility fix.
- Reimplement Android 17 Keyguard visibility policy in the module: this would still diverge from OPlus-specific privacy/profile behavior and is unnecessary for the narrow OEM defect.
