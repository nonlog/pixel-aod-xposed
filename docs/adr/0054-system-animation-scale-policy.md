# ADR 0054: Apply one system animation-scale policy to all module-owned motion

Date: 2026-08-22
Status: Accepted

## Context

ADR 0039 already requires the Pixel/COUI charge animation to honor the Android animator-scale preference, but the module also owns clock-size morphs, lockscreen/AOD presentation transitions, contextual/media transitions, and optional visual effects. Treating only one animation as accessibility-aware would produce inconsistent behavior when the user disables or scales system animations.

## Decision

Adopt one **system animation-scale policy** for module-owned presentation motion.

1. Observe the current Android/SystemUI animation-scale preference through a stable system input.
2. When animations are disabled, all module-owned presentation animations snap to their deterministic terminal state.
3. When animations are enabled with a non-default scale, adjust module animation timing consistently with that preference or its validated SystemUI equivalent.
4. Apply the policy to clock morphs, lockscreen/AOD presentation transitions, contextual/media transitions, charge animation, optional success ripple, and other module-owned visual motion.
5. Do not alter timing for vendor-owned UDFPS, HBM/local-HBM, pulse foreground, or other native authentication/system animations.

## Consequences

- Android's remove/reduce animation preference is respected consistently across Pixel AOD.
- Individual features no longer need unrelated animator-scale handling.
- Motion tests can validate a single disabled/default/scaled contract.

## Rejected alternatives

- Honor animation scale only for charge animation: creates inconsistent accessibility behavior.
- Ignore system animation scale: leaves module motion active when the user explicitly disables Android animations.
