# ADR 0063: Hand AOD wake directly to Gone when SystemUI does

Date: 2026-08-22
Status: Accepted

## Context

Not every wake from AOD should pass through a visible lockscreen. Native Keyguard can decide to transition directly from Dozing/AOD to the unlocked Gone scene, for example when authentication is already satisfied or Keyguard does not need to remain visible. If Pixel always attaches its lockscreen replacement first, those native direct-unlock paths can flash the lockscreen for one or more frames.

## Decision

Add a read-only **native direct-wake-to-Gone handoff**.

1. Observe a stable OPlus/SystemUI decision that the current AOD/Dozing wake is transitioning directly to Gone/unlocked content.
2. Once that decision is authoritative, tear down Pixel ambient presentation without attaching Pixel Lockscreen or starting the normal AOD-to-Lockscreen bridge/enter animation.
3. Do not infer direct-to-Gone from screen-on timing, face-presence guesses, or a short-lived unlocked flag.
4. If the native transition is cancelled or redirected, follow the next authoritative native Keyguard scene.
5. Ordinary AOD-to-Lockscreen transitions keep the existing stable Pixel transition and weight/morph animation unchanged.

## Consequences

- Direct face/trust/no-lock wake paths do not flash the Pixel lockscreen.
- Normal AOD-to-Lockscreen animation remains the protected default path.
- Scene authority stays with OPlus/SystemUI instead of module timing heuristics.

## Rejected alternatives

- Wait until Gone is fully reached before hiding Pixel: can still show an unwanted transient lockscreen frame.
- Force every wake through Pixel Lockscreen: conflicts with native Keyguard transition semantics and degrades direct unlock.
