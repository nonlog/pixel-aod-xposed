# Pixel AOD Xposed Agent Rules

These rules apply to automated work in this repository.

## Scope and priority

- User instructions take priority over this file.
- Keep this file limited to long-lived repository rules. Put current branch, worktree, device, APK hash, test stage, and next-step state in the local handoff file.
- Keep the root `AGENTS.md` identical across active branches and worktrees. Store branch-specific progress in `.local/CHATGPT_WEB_HANDOFF.md`.

## Before changing code

- Locate the relevant implementation, callers, tests, and existing documentation before editing.
- For multi-step work, state a short plan and then execute it.
- Preserve unrelated dirty-worktree changes. Do not rewrite historical commits.

## Pixel AOD invariants

- Preserve the resolved controller target and the existing partial/panoramic AOD policy.
- Do not reintroduce the withdrawn media-only Large-AOD override.
- Keep behavior compatible with the supported Android and OPlus/SystemUI versions documented in the repository.
- Keep hooks narrow and fail open where a missing or changed SystemUI target would otherwise affect normal device behavior.

## Build, install, and verification

- Verify the relevant Gradle module and test target before building.
- Keep build, install, log evidence, and user-visible device behavior as separate results.
- For device verification, record the exact connected device and APK identity in the local handoff, never in this file.
- Do not claim a device behavior is fixed from a successful build or install alone.
- Update the repository changelog or tracked documentation when behavior, compatibility, or configuration changes.

## Security and sensitive data

- Never commit credentials, tokens, private URLs containing secrets, generated token files, or raw device dumps.
- Redact secrets and personal identifiers from logs, test output, screenshots, and handoff documents.
- Do not weaken Android, LSPosed, or module security checks to make a test pass.

## Git and delivery

- Agent-created commits use `Codex <codex@openai.com>` as author and committer.
- Do not push unless the user explicitly requests it in the current context.
- Before delivery, run the smallest relevant build/test check and report whether device verification was performed.
