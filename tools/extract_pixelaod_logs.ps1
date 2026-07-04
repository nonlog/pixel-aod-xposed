param(
    [string]$Serial = "",
    [datetime]$Start = (Get-Date).AddMinutes(-10),
    [datetime]$End = (Get-Date),
    [string]$Adb = "D:\enviroment\ADB\adb.exe",
    [string]$Pattern = "PixelAodOPlus|dev\.codex\.pixelaod",
    [string]$Output = ""
)

$ErrorActionPreference = "Stop"

function Resolve-DeviceSerial {
    param([string]$RequestedSerial)
    if ($RequestedSerial) {
        return $RequestedSerial
    }
    $devices = & $Adb devices | Select-String -Pattern "`tdevice$" | ForEach-Object {
        ($_ -split "`t")[0]
    }
    if ($devices.Count -ne 1) {
        throw "Expected exactly one online adb device, found $($devices.Count): $($devices -join ', ')"
    }
    return $devices[0]
}

function Parse-LogTime {
    param([string]$Line)
    if ($Line -match '^\[\s*(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3})') {
        return [datetime]::ParseExact($Matches[1], "yyyy-MM-ddTHH:mm:ss.fff",
            [System.Globalization.CultureInfo]::InvariantCulture)
    }
    if ($Line -match '^(\d{2})-(\d{2}) (\d{2}:\d{2}:\d{2}\.\d{3})') {
        $value = "{0}-{1}-{2} {3}" -f $Start.Year, $Matches[1], $Matches[2], $Matches[3]
        return [datetime]::ParseExact($value, "yyyy-MM-dd HH:mm:ss.fff",
            [System.Globalization.CultureInfo]::InvariantCulture)
    }
    return $null
}

function In-Window {
    param([string]$Line)
    $time = Parse-LogTime $Line
    return $time -ne $null -and $time -ge $Start -and $time -le $End
}

function Select-PixelAodLines {
    param([string[]]$Lines)
    foreach ($line in $Lines) {
        if ($line -match $Pattern -and (In-Window $line)) {
            $line
        }
    }
}

$Serial = Resolve-DeviceSerial $Serial
$results = New-Object System.Collections.Generic.List[string]
$results.Add("== Pixel AOD logs serial=$Serial start=$($Start.ToString('s')) end=$($End.ToString('s')) ==")

$logcatLines = & $Adb -s $Serial logcat -d -v time
$logcatMatches = @(Select-PixelAodLines $logcatLines)
$results.Add("== adb logcat matches=$($logcatMatches.Count) ==")
$results.AddRange([string[]]$logcatMatches)

$remoteLogs = & $Adb -s $Serial shell su -c "ls -1 /data/adb/lspd/log/modules_*.log 2>/dev/null" |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ }

foreach ($remoteLog in $remoteLogs) {
    $lines = & $Adb -s $Serial exec-out su -c "cat '$remoteLog'"
    $matches = @(Select-PixelAodLines $lines)
    if ($matches.Count -gt 0) {
        $results.Add("== $remoteLog matches=$($matches.Count) ==")
        $results.AddRange([string[]]$matches)
    }
}

if ($Output) {
    $directory = Split-Path -Parent $Output
    if ($directory) {
        New-Item -ItemType Directory -Force -Path $directory | Out-Null
    }
    $results | Set-Content -Path $Output -Encoding UTF8
    Write-Output $Output
} else {
    $results
}
