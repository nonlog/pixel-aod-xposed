param(
    [string]$Adb = "D:\Android\sdk\platform-tools\adb.exe",
    [string]$OutRoot = "D:\Downloads\Xposed_test\screenshots",
    [string]$LogRoot = "D:\Downloads\Xposed_test",
    [string]$ScrcpyPath = "D:\Programs\Scoop\apps\escrcpy\current\resources\extra\win\scrcpy\scrcpy.exe",
    [string]$Pin = "0000",
    [switch]$RestartSystemUi,
    [switch]$Install,
    [switch]$PostNotification,
    [switch]$ClearNotification,
    [switch]$Aod,
    [switch]$Lockscreen,
    [switch]$UnlockShade,
    [switch]$CollapseShade,
    [switch]$Scrcpy,
    [switch]$Logs,
    [switch]$All
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path -LiteralPath $Adb)) {
    throw "adb not found: $Adb"
}

$repo = Split-Path -Parent $PSScriptRoot
$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$outDir = Join-Path $OutRoot "verify_$stamp"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

function Invoke-Adb {
    param([string[]]$AdbArgs)
    & $Adb @AdbArgs
    if ($LASTEXITCODE -ne 0) {
        throw "adb failed: $($AdbArgs -join ' ')"
    }
}

function Invoke-AdbShell {
    param([string]$Command)
    Invoke-Adb -AdbArgs @("shell", $Command)
}

function Save-Screenshot {
    param([string]$Name)
    $path = Join-Path $outDir $Name
    cmd.exe /c """$Adb"" exec-out screencap -p > ""$path"""
    if ($LASTEXITCODE -ne 0) {
        throw "screencap failed: $Name"
    }
    Write-Host "screenshot: $path"
}

function Ensure-Awake {
    Invoke-AdbShell "input keyevent KEYCODE_WAKEUP"
    Start-Sleep -Milliseconds 450
}

function Sleep-Device {
    Invoke-AdbShell "input keyevent KEYCODE_SLEEP"
    Start-Sleep -Milliseconds 900
}

function Unlock-Device {
    Ensure-Awake
    Invoke-AdbShell "input swipe 540 2100 540 900 220"
    Start-Sleep -Milliseconds 250
    foreach ($digit in $Pin.ToCharArray()) {
        Invoke-AdbShell "input text $digit"
        Start-Sleep -Milliseconds 60
    }
    Invoke-AdbShell "input keyevent KEYCODE_ENTER"
    Start-Sleep -Milliseconds 900
}

function Expand-Shade {
    Invoke-AdbShell "input swipe 540 0 540 1800 450"
    Start-Sleep -Milliseconds 700
}

function Collapse-Shade {
    Invoke-AdbShell "input keyevent KEYCODE_BACK"
    Start-Sleep -Milliseconds 450
}

if ($All) {
    $RestartSystemUi = $true
    $PostNotification = $true
    $Aod = $true
    $Lockscreen = $true
    $UnlockShade = $true
    $Logs = $true
}

if ($Scrcpy) {
    if (-not (Test-Path -LiteralPath $ScrcpyPath)) {
        throw "scrcpy not found: $ScrcpyPath"
    }
    Start-Process -FilePath $ScrcpyPath -ArgumentList @(
        "--stay-awake",
        "--turn-screen-on",
        "--window-title", "Pixel AOD verify"
    )
    Start-Sleep -Seconds 1
}

if ($Install) {
    $apk = Join-Path $repo "app\build\outputs\apk\debug\app-debug.apk"
    if (-not (Test-Path -LiteralPath $apk)) {
        throw "APK not found: $apk"
    }
    Invoke-Adb -AdbArgs @("install", "-r", $apk)
}

if ($RestartSystemUi) {
    Invoke-Adb -AdbArgs @("logcat", "-c")
    Invoke-Adb -AdbArgs @("shell", "su", "-c", "pkill -f com.android.systemui")
    Start-Sleep -Seconds 10
}

if ($ClearNotification) {
    Invoke-Adb -AdbArgs @(
        "shell", "am", "broadcast",
        "-n", "dev.codex.pixelaod/.TestNotificationReceiver",
        "-a", "dev.codex.pixelaod.CLEAR_TEST_NOTIFICATION"
    )
    Start-Sleep -Milliseconds 500
}

if ($PostNotification) {
    Invoke-Adb -AdbArgs @(
        "shell", "am", "broadcast",
        "-n", "dev.codex.pixelaod/.TestNotificationReceiver",
        "-a", "dev.codex.pixelaod.TEST_NOTIFICATION",
        "--es", "title", "PixelAOD",
        "--es", "text", "Lockcard"
    )
    Start-Sleep -Milliseconds 700
}

if ($Aod) {
    Sleep-Device
    Start-Sleep -Seconds 3
    Save-Screenshot "aod.png"
}

if ($Lockscreen) {
    Ensure-Awake
    Start-Sleep -Milliseconds 900
    Save-Screenshot "lockscreen.png"
}

if ($UnlockShade) {
    Unlock-Device
    Expand-Shade
    Save-Screenshot "shade_unlocked.png"
}

if ($CollapseShade) {
    Collapse-Shade
}

if ($Logs) {
    $logPath = Join-Path $LogRoot "aod_logs_verify_$stamp.txt"
    & $Adb logcat -d -v time -s PixelAodOPlus > $logPath
    if ($LASTEXITCODE -ne 0) {
        throw "logcat failed"
    }
    Write-Host "logs: $logPath"
}

Write-Host "output: $outDir"
