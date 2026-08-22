# ADR 0011: Native contextual pass-through boundary

Date: 2026-08-22
Status: Accepted

## Context

Pixel/Google devices can surface contextual information whose provider/backend is not part of AOSP, such as some Now Playing, commute, delivery, flight, or Google-specific Smartspace data. ADR 0003 already defines a read-only Smartspace adapter, but the project still needs a scope boundary for non-AOSP native targets.

## Decision

Adopt a **native contextual pass-through boundary**.

1. Pixel AOD may render contextual targets already produced by OPlus/SystemUI/Google services when they reach a stable lock-screen/AOD target surface.
2. Normalize those targets into the module presentation model and apply privacy/expiry/deduplication policy.
3. Do not reverse-engineer, scrape, or recreate Pixel-private/Google-private provider backends merely to manufacture missing targets.
4. Existing explicit module-owned sources such as Breezy Weather and Calendar remain allowed.
5. A missing private target is not an AOSP-parity defect unless the supported vendor stack already produces it and Pixel AOD fails to consume it.

## Consequences

- The module benefits from richer contextual data when the device already provides it.
- Project scope stays focused on presentation/adaptation rather than cloning Google service backends.
- Future private-provider work requires an explicit new product decision.

## Rejected alternatives

- Restrict all contextual data to pure AOSP producers: unnecessarily discards useful native targets.
- Pursue complete Pixel private-feature parity by reverse-engineering providers: unbounded scope and outside the current architecture goal.
