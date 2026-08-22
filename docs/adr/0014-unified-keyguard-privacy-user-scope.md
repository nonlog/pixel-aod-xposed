# ADR 0014: Unified Keyguard privacy and Android user scope

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD already applies some sensitive-content redaction, but Calendar, future Smartspace targets, Live Updates, media, and caches do not yet share one explicit selected-user/profile ownership contract. AOD must never retain personal content across Android user or work-profile boundaries.

## Decision

Implement **unified Keyguard privacy and user scope** for all personal AOD content.

1. Bind notification, media, Calendar, Smartspace/contextual, and Live Update state to the currently selected Android user.
2. Immediately invalidate old-user personal caches on user switch.
3. Withhold work-profile content while that profile is quiet, locked, unavailable, or otherwise hidden by system policy.
4. Apply Keyguard sensitive-content privacy consistently to all personal text and metadata.
5. Non-personal content such as weather may remain visible when normal presentation gates permit it.
6. Module settings must not override system privacy to reveal content the system has hidden.

## Consequences

- Smartspace and Live Update expansion cannot introduce cross-user or work-profile stale-data leaks.
- Privacy policy becomes one shared gate instead of provider-specific approximations.
- User/profile lifecycle changes require deterministic cache invalidation and refresh behavior.

## Rejected alternatives

- Let every provider implement privacy/user handling independently: encourages drift and stale cross-user state.
- Add module privacy overrides that can reveal system-hidden content: violates Keyguard ownership and security expectations.
