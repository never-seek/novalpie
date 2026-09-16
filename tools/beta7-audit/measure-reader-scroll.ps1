param(
    [Parameter(Mandatory)][string]$EvidenceName,
    [Parameter(Mandatory)][string]$BookId,
    [ValidateSet('warm','cold-launch')][string]$CacheState = 'warm',
    [string]$Serial = '127.0.0.1:16384',
    [string]$Adb = 'C:\Users\86188\AppData\Local\Android\Sdk\platform-tools\adb.exe'
)
$ErrorActionPreference = 'Stop'
if ($EvidenceName -notmatch '^[a-zA-Z0-9_-]+$' -or $BookId -notmatch '^\d+$') { throw 'Invalid evidence identity' }
$package = 'com.novalpie.app.debug'
$foreground = (& $Adb -s $Serial shell dumpsys activity activities | Select-String 'mResumedActivity|topResumedActivity') -join ' '
if ($foreground -notmatch [regex]::Escape($package)) { throw 'Open the NovalPie reader before measuring' }
$apk = ((& $Adb -s $Serial shell pm path $package | Select-Object -First 1) -replace '^package:', '').Trim()
if ($apk -notmatch '^/data/app/.+/base\.apk$') { throw 'Unexpected installed APK path' }
$hash = ((& $Adb -s $Serial shell sha256sum $apk) -split '\s+')[0]
$display = (& $Adb -s $Serial shell wm size) -join ' '
$density = (& $Adb -s $Serial shell wm density) -join ' '
$font = (& $Adb -s $Serial shell settings get system font_scale) -join ''
if ($display -notmatch '540x1098') { throw 'This fixed-gesture script requires the documented 540x1098 viewport' }
$results = @()
for ($round = 1; $round -le 3; $round++) {
    & $Adb -s $Serial shell dumpsys gfxinfo $package reset | Out-Null
    foreach ($direction in @('down','down','down','up','up','up')) {
        if ($direction -eq 'down') { & $Adb -s $Serial shell input swipe 270 870 270 270 650 | Out-Null }
        else { & $Adb -s $Serial shell input swipe 270 270 270 870 650 | Out-Null }
        if ($LASTEXITCODE -ne 0) { throw 'Swipe failed' }
    }
    $raw = (& $Adb -s $Serial shell dumpsys gfxinfo $package framestats) -join "`n"
    $total = [regex]::Match($raw, 'Total frames rendered:\s*(\d+)')
    $jank = [regex]::Match($raw, 'Janky frames:\s*(\d+)\s*\(([\d.]+)%\)')
    if (!$total.Success -or !$jank.Success) { throw 'Missing device frame statistics' }
    $results += [ordered]@{ round=$round; frames=[int]$total.Groups[1].Value; jankyFrames=[int]$jank.Groups[1].Value;
        jankPercent=[double]$jank.Groups[2].Value; p90ms=[regex]::Match($raw,'90th percentile:\s*(\d+)ms').Groups[1].Value }
    $rawPath = "D:\NovalPie\agent-bridge\artifacts\beta7-device\$EvidenceName-round$round-gfx.txt"
    $raw | Set-Content -LiteralPath $rawPath -Encoding utf8
}
$record = [ordered]@{ at=(Get-Date).ToString('o'); bookId=$BookId; cacheState=$CacheState; app=$package; apkSha256=$hash;
    display=$display; density=$density; fontScale=$font; gesture='3 upward + 3 downward, x270 y870/270, 650ms'; rounds=$results;
    warmTextThresholdPercent=5; meetsWarmTextTarget=($CacheState -eq 'warm' -and @($results | Where-Object { $_.jankPercent -gt 5 -or $_.frames -lt 30 }).Count -eq 0);
    physicalHighRefreshVerified=$false; sourceCacheCleared=$false }
$record | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath "D:\NovalPie\agent-bridge\artifacts\beta7-device\$EvidenceName.json" -Encoding utf8
$record | ConvertTo-Json -Depth 5 -Compress
