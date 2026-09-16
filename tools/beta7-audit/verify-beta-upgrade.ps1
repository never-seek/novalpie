param(
    [string]$Adb = 'C:\Users\86188\AppData\Local\Android\Sdk\platform-tools\adb.exe',
    [string]$Serial = '127.0.0.1:16384',
    [string]$EvidenceName = 'beta7-upgrade',
    [switch]$VerifyOnly
)
$ErrorActionPreference = 'Stop'
if ($EvidenceName -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Use a simple evidence name' }
$deviceState = & $Adb -s $Serial get-state 2>$null
if ($LASTEXITCODE -ne 0 -or $deviceState -ne 'device') { throw 'MuMu is not connected; no installation or verification was attempted' }
$boot = & $Adb -s $Serial shell getprop sys.boot_completed
if ($LASTEXITCODE -ne 0 -or ($boot -join '').Trim() -ne '1') { throw 'MuMu is still booting; wait before verification' }
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$baseline = 'D:\NovalPie\agent-bridge\artifacts\20260905-beta6-reader-header-tail-release-upload\NovalPie-native-2.0.0-beta6-debug.apk'
if ((Get-FileHash -LiteralPath $baseline -Algorithm SHA256).Hash -ne '30F07AC07FBAA0D79AA0FF8EBD126FF82B55456D392825B32D62E7BD9707D6C3') { throw 'Baseline is not the verified Beta6 release' }
& (Join-Path $PSScriptRoot 'verify-beta-artifact.ps1') -EvidenceName "$EvidenceName-artifact"
$beta = Join-Path $repo 'app\build\outputs\apk\beta\app-beta.apk'
$test = Join-Path $repo 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
$services = & $Adb -s $Serial shell dumpsys activity services com.novalpie.app.debug
if ($LASTEXITCODE -ne 0) { throw 'Could not inspect NovalPie services; no installation attempted' }
if (($services -join "`n") -match 'ServiceRecord\{') { throw 'Stop here: cannot replace an app with active background work' }
$path = ((& $Adb -s $Serial shell pm path com.novalpie.app.debug | Select-Object -First 1) -replace '^package:', '').Trim()
if ($path -notmatch '^/data/app/.+/base\.apk$') { throw 'Cannot identify the current package' }
$before = ((& $Adb -s $Serial shell sha256sum $path) -split '\s+')[0]
if (!$VerifyOnly) {
    & $Adb -s $Serial install -r $baseline
    if ($LASTEXITCODE -ne 0) { throw 'Baseline install failed; do not uninstall, clear data, or force a downgrade' }
}
& $Adb -s $Serial install -r $test
if ($LASTEXITCODE -ne 0) { throw 'Black-box test APK install failed' }
function Run-BlackBox([string]$Class, [string]$Phase) {
    $result = & $Adb -s $Serial shell am instrument -w -r -e phase $Phase com.novalpie.app.debug.test/com.novalpie.nativeapp.ui.BetaSdkInstrumentation 2>&1
    $text = $result -join "`n"
    $log = Join-Path 'D:\NovalPie\agent-bridge\artifacts\beta7-device' "$EvidenceName-$Phase.log"
    $text | Set-Content -LiteralPath $log -Encoding utf8
    if ($text -notmatch "BETA7_SDK_OK $Phase" -or $text -match 'BETA7_SDK_FAILED|INSTRUMENTATION_FAILED|Process crashed') { throw "Black-box $Phase failed: $log" }
}
if (!$VerifyOnly) {
    Run-BlackBox 'com.novalpie.nativeapp.ui.BetaUpgradePrepareTest' 'prepare'
    & $Adb -s $Serial install -r $beta
    if ($LASTEXITCODE -ne 0) { throw 'Beta7 cover installation failed; never uninstall to bypass it' }
}
$path = ((& $Adb -s $Serial shell pm path com.novalpie.app.debug | Select-Object -First 1) -replace '^package:', '').Trim()
$installed = ((& $Adb -s $Serial shell sha256sum $path) -split '\s+')[0].ToLowerInvariant()
if ($installed -ne (Get-FileHash -LiteralPath $beta -Algorithm SHA256).Hash.ToLowerInvariant()) { throw 'Installed package differs from checked R8 artifact' }
Run-BlackBox 'com.novalpie.nativeapp.ui.BetaUpgradeVerifyTest' 'verify'
# run-as is intentionally unavailable in a non-debuggable beta; the signed black-box test is the
# verifier. No raw app preferences or session values are exported by this helper.
[pscustomobject]@{baselineSha256=(Get-FileHash -LiteralPath $baseline -Algorithm SHA256).Hash.ToLowerInvariant();installedBetaSha256=$installed;
    priorDevelopmentSha256=$before;coverInstall=$true;rawDataExported=$false;blackBoxPassed=$true;allBusinessRuntimeVerified=$false;evidence=$EvidenceName} |
    ConvertTo-Json -Compress | Tee-Object -FilePath (Join-Path 'D:\NovalPie\agent-bridge\artifacts\beta7-device' "$EvidenceName.json")
