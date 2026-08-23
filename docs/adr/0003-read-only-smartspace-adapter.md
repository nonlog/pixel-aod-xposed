# ADR 0003: Read-only Smartspace adapter for contextual AOD content

Date: 2026-08-22
Status: Accepted

## Context

Pixel AOD currently owns Weather, Forecast, Weather Alert and Calendar contextual sources. Modern AOSP/Pixel lock-screen/AOD presentation can receive a broader set of Smartspace/At a Glance targets such as alarm/timer and other contextual cards. Reimplementing each source independently would duplicate native SystemUI data pipelines and turn the module into a separate Smartspace backend.

ADR 0001 keeps OPlus/SystemUI authoritative for vendor lifecycle and native services. The module's goal is presentation parity, not provider duplication.

## Decision

Implement a **read-only Smartspace adapter**.

1. Prefer stable OPlus/SystemUI lock-screen/AOD Smartspace targets when they are available.
2. Normalize native targets into a module-owned `ContextualTarget` model before rendering.
3. Keep AOD consumption read-only: the module does not invoke arbitrary Smartspace actions while the panel is in AOD.
4. Preserve existing module-owned Weather/Forecast/Alert/Calendar sources as fallbacks or supplements when an equivalent native target is absent or unusable.
5. Deduplicate equivalent native/module-owned targets before presentation.
6. Apply lock-screen privacy, profile and sensitivity policy before exposing target text on AOD.

## Consequences

- Alarm/Timer and future contextual content can often be obtained without building new providers.
- Presentation stays decoupled from vendor target classes through a small normalized model.
- ROMs without a stable native Smartspace target surface can fall back to current module-owned sources instead of failing the AOD surface.
- The adapter needs explicit target identity, priority, expiry and privacy semantics so stale cards cannot persist across AOD sessions.

## Rejected alternatives

- Hand-code every contextual source independently: simple at first, but duplicates native providers and scales poorly.
- Implement a complete Pixel/Google Smartspace backend: much larger scope, includes non-AOSP/private data sources, and conflicts with the project's presentation-focused ownership boundary.

## M9 P1-S19 implementation note — 2026-08-23

- Exact current-OOS SystemUI owns a real `LockscreenSmartspaceController` and framework lockscreen `SmartspaceSession` lifecycle.
- The final read-only seam is `LockscreenSmartspaceController$sessionListener$1#onTargetsAvailable(List)` **after** the original callback. SystemUI first applies its own selected-user, managed-profile and sensitive-content filter, writes that post-filter list to `recentSmartspaceData`, and Pixel reads only `peekLast()` from that filtered history.
- Pixel AOD never creates/requests the Smartspace session, builds a native Smartspace view, invokes `requestSmartspaceUpdate()`, registers a provider/plugin listener, or runs target actions.
- The normalized adapter retains native target ID/feature/expiry/sensitivity metadata, adds only a fail-safe TTL when expiry is absent, applies existing AOD privacy/suppression again, skips duplicate current-weather feature type 1, and maps native calendar feature type 2 onto the module calendar semantic key for S18 deduplication.
- A higher-level `BcSmartspaceDataPlugin` listener candidate was explicitly rejected after device runtime proved that controller field is `null` on the exact current build.
- Current-device source state is absent/inactive: `Region Samplers: 0`, `Recent BC Smartspace Targets: No data`, and a real Keyguard lifecycle produced no target callback. This is accepted as the correct fail-open result; no synthetic target is used to claim native-content validation.
