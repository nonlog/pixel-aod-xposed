# ADR 0019: Keep Android 17 clock-registry parity out of product scope

Date: 2026-08-22
Status: Accepted

## Context

Modern Android SystemUI includes clock registry/provider infrastructure and theme-picker integration for selecting multiple clock faces. Pixel AOD for OPlus is currently built around one persistent COUI/Pixel clock presentation owner, and M8 intentionally converged the runtime away from competing clock-owner paths. Reintroducing a registry would expand the product into a theme engine and weaken the single-owner architecture without improving the AOD lifecycle/content parity that defines this project.

## Decision

Keep **multi-clock registry and clock-picker parity out of M9 scope**.

1. Retain one Pixel/COUI primary clock presentation owner.
2. Define parity around AOD lifecycle, transitions, contextual content, privacy, pulse behavior, and low-power presentation rather than clock-theme selection.
3. Do not implement a module-owned clone of AOSP `ClockRegistry` or `ClockProviderPlugin` selection infrastructure for M9.
4. Do not couple runtime correctness to private OPlus clock-picker internals.
5. Revisit alternative clock faces only as a separate product feature with its own ownership and compatibility design.

## Consequences

- M8's single clock-owner convergence remains intact.
- M9 can focus on functional AOD parity rather than theme-picker scope expansion.
- Lack of multiple clock faces is an explicit product boundary, not a parity bug.

## Rejected alternatives

- Build a small module clock registry now: expands scope and reintroduces multi-owner complexity.
- Mirror the OPlus/private clock picker: strongly version-sensitive and outside the project's current presentation goal.
