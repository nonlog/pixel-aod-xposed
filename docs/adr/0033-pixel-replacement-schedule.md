# ADR 0033: Treat the module schedule as a Pixel replacement schedule

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD already exposes an independent schedule. Under the vendor-delegated lifecycle architecture, that schedule cannot legitimately decide whether the panel remains in Doze or prevent OPlus from ending AOD. The useful product behavior is instead to decide when Pixel presentation replaces an already-valid vendor AOD.

## Decision

Define the module schedule as a **Pixel replacement schedule**.

1. Inside the module schedule, Pixel presentation is eligible only when native OPlus/SystemUI AOD policy and lifecycle also permit it.
2. Outside the module schedule, release Pixel stock-view suppression and return presentation ownership to the native OPlus AOD surface.
3. Do not use the module schedule to rewrite `DreamService` screen state, suppress vendor hide callbacks, or otherwise keep Doze alive.
4. Users who want the panel/AOD itself disabled during certain hours use the OPlus system AOD schedule.
5. Trigger/auth paths remain governed by their own vendor capabilities rather than being implicitly converted into continuous AOD.

## Consequences

- Existing module scheduling remains useful without becoming a second power policy.
- Schedule boundaries become clean presentation handoffs between Pixel and native OPlus AOD.
- Tests must verify both directions of ownership restoration without blank or duplicate AOD frames.

## Rejected alternatives

- Delete the module schedule entirely: removes a useful replacement-presentation preference unnecessarily.
- Keep using it to control Doze lifetime: conflicts with ADR 0001 and native AOD enable authority.
