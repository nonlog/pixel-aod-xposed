# COUI reference policy

The full reverse-engineered COUI/OPlus reference snapshot is intentionally excluded from this public repository.

## Private source

The complete `coui-2.5.0-260802` snapshot is stored in the private repository `nonlog/pixel-aod-coui-reference`.

## What belongs here

Only durable engineering conclusions may be copied into Pixel AOD public source control:

- ADRs describing observed COUI behavior and ownership boundaries.
- Stable constants and geometry/animation semantics needed by the module.
- Interface/contract notes that are necessary to maintain compatibility.
- Narrow excerpts rewritten as project-owned behavior descriptions rather than vendored reference source.
- Regression tests that encode the required behavior.

Do not vendor the full COUI source tree, decompiled reference classes, or private-reference repository contents into `pixel-aod-xposed`.

When a reference finding changes implementation behavior, document the conclusion in `docs/` and protect it with an automated test whenever practical so normal development does not require local access to the private reference tree.
