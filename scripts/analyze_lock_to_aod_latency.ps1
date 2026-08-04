param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string[]] $Path,

    [int] $ThresholdMs = 150,

    [int] $PairWindowMs = 2000,

    [ValidateSet('screen-off', 'dream-start')]
    [string] $StartKind = 'screen-off',

    [switch] $RequireNativeDrawSuppression
)

$ErrorActionPreference = 'Stop'

$resolvedFiles = foreach ($item in $Path) {
    Get-Item -LiteralPath $item -ErrorAction Stop
}

$timestampPattern = '^\[ (?<timestamp>\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3})\s+'
$events = [System.Collections.Generic.List[object]]::new()
$seen = [System.Collections.Generic.HashSet[string]]::new([System.StringComparer]::Ordinal)

foreach ($file in $resolvedFiles) {
    foreach ($line in [System.IO.File]::ReadLines($file.FullName)) {
        if ($line -notmatch $timestampPattern) {
            continue
        }

        $timestampText = $Matches.timestamp
        $kind = $null
        if ($line.Contains('FOD AOD diagnostic source=OnScreenFingerprintUiMech#startToAnimInDream()')) {
            $kind = 'dream-start'
        } elseif ($line.Contains('noted Pixel AOD screen-off')) {
            $kind = 'screen-off'
        } elseif ($line.Contains('presentAod weightStart source=ClockPlugin#render') -or
                $line.Contains('presentAod weightStart source=WakefulnessLifecycle#dispatchStartedGoingToSleep#ClockPlugin-pre-present')) {
            $kind = 'present-aod'
        } elseif ($line.Contains('ClockPlugin native draw suppression hook installed')) {
            $kind = 'native-draw-hook'
        } elseif ($line.Contains('bound persistent ClockPlugin native draw suppression')) {
            $kind = 'native-draw-binding'
        } elseif ($line.Contains('ClockPlugin host sync source=ClockPlugin#render')) {
            $kind = 'render'
        } elseif ($line.Contains('committed AOD info stack layout')) {
            $kind = 'layout'
        } else {
            continue
        }

        $key = "$timestampText|$kind|$line"
        if (-not $seen.Add($key)) {
            continue
        }

        $events.Add([pscustomobject]@{
            Timestamp = [datetime]::ParseExact(
                $timestampText,
                'yyyy-MM-ddTHH:mm:ss.fff',
                [Globalization.CultureInfo]::InvariantCulture)
            Kind = $kind
        })
    }
}

$ordered = @($events | Sort-Object Timestamp)
$starts = @($ordered | Where-Object Kind -eq $StartKind)
$presents = @($ordered | Where-Object Kind -eq 'present-aod')
$pairs = [System.Collections.Generic.List[object]]::new()

foreach ($start in $starts) {
    $deadline = $start.Timestamp.AddMilliseconds($PairWindowMs)
    $present = $presents |
        Where-Object { $_.Timestamp -ge $start.Timestamp -and $_.Timestamp -le $deadline } |
        Select-Object -First 1
    if ($null -eq $present) {
        continue
    }

    $latencyMs = [int][math]::Round(($present.Timestamp - $start.Timestamp).TotalMilliseconds)
    $renderCount = @($ordered | Where-Object {
        $_.Kind -eq 'render' -and
        $_.Timestamp -ge $start.Timestamp -and
        $_.Timestamp -le $present.Timestamp
    }).Count
    $layoutCount = @($ordered | Where-Object {
        $_.Kind -eq 'layout' -and
        $_.Timestamp -ge $start.Timestamp -and
        $_.Timestamp -le $present.Timestamp
    }).Count
    $pairs.Add([pscustomobject]@{
        Start = $start.Timestamp.ToString('HH:mm:ss.fff')
        Present = $present.Timestamp.ToString('HH:mm:ss.fff')
        LatencyMs = $latencyMs
        RenderCount = $renderCount
        LayoutCount = $layoutCount
        Verdict = if ($latencyMs -le $ThresholdMs) { 'PASS' } else { 'FAIL' }
    })
}

if ($pairs.Count -eq 0) {
    Write-Error "No $StartKind -> presentAod pair found within ${PairWindowMs}ms."
}

$pairs | Format-Table -AutoSize
$latencies = @($pairs | ForEach-Object LatencyMs | Sort-Object)
$average = [int][math]::Round(($latencies | Measure-Object -Average).Average)
$p95Index = [math]::Max(0, [math]::Ceiling($latencies.Count * 0.95) - 1)
$failed = @($pairs | Where-Object Verdict -eq 'FAIL').Count

Write-Output ''
Write-Output ("pairs={0} failed={1} thresholdMs={2} averageMs={3} p95Ms={4}" -f `
    $pairs.Count, $failed, $ThresholdMs, $average, $latencies[$p95Index])

if ($RequireNativeDrawSuppression) {
    $hookCount = @($ordered | Where-Object Kind -eq 'native-draw-hook').Count
    $bindingCount = @($ordered | Where-Object Kind -eq 'native-draw-binding').Count
    $nativeDrawVerdict = if ($hookCount -gt 0 -and $bindingCount -gt 0) { 'PASS' } else { 'FAIL' }
    Write-Output ("nativeDrawSuppression={0} hooks={1} bindings={2}" -f `
        $nativeDrawVerdict, $hookCount, $bindingCount)
    if ($nativeDrawVerdict -eq 'FAIL') {
        $failed++
    }
}

if ($failed -gt 0) {
    exit 1
}
