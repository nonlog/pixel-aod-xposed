Set-Location -LiteralPath 'D:\Downloads\Xposed_test\pixel-aod-xposed'

$prompt = Get-Content -LiteralPath '.\NEW_SESSION_START_PROMPT.md' -Raw

codex --cd 'D:\Downloads\Xposed_test\pixel-aod-xposed' --sandbox workspace-write --ask-for-approval on-request $prompt
