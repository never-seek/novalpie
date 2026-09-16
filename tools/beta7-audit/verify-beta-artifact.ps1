param(
    [string]$BuildTools = 'C:\Users\86188\AppData\Local\Android\Sdk\build-tools\35.0.0',
    [string]$EvidenceName = 'beta-artifact'
)
$ErrorActionPreference = 'Stop'
if ($EvidenceName -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Use a simple evidence name' }
$repo = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$apk = Join-Path $repo 'app\build\outputs\apk\beta\app-beta.apk'
if (!(Test-Path -LiteralPath $apk)) { throw 'Build the optimized beta APK first' }
$signature = & (Join-Path $BuildTools 'apksigner.bat') verify --verbose --print-certs $apk 2>&1
if ($LASTEXITCODE -ne 0) { throw 'APK signature verification failed' }
$certificate = [regex]::Match(($signature -join "`n"), 'Signer #1 certificate SHA-256 digest: ([a-fA-F0-9]+)').Groups[1].Value.ToLowerInvariant()
if ($certificate -ne 'cc7d6fe6844d4a4ee510d65b185ac5e32a642faa9bfb819df75d51be4c607047') { throw 'Signer differs from the published Beta6; do not uninstall or install it' }
$badging = & (Join-Path $BuildTools 'aapt.exe') dump badging $apk
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect APK manifest' }
$text = $badging -join "`n"
$package = [regex]::Match($text, "package: name='([^']+)' versionCode='([^']+)' versionName='([^']+)'")
if (!$package.Success) { throw 'Missing package metadata' }
if ($package.Groups[1].Value -ne 'com.novalpie.app.debug') { throw 'Wrong upgrade package id' }
if ([long]$package.Groups[2].Value -le 2026090101) { throw 'Version code must increase from Beta6' }
if ($package.Groups[3].Value -ne '2.0.0-native-beta7') { throw 'Wrong beta version name' }
if ($text -match '(?m)^application-debuggable') { throw 'Optimized beta must not be debuggable' }
$xml = & (Join-Path $BuildTools 'aapt.exe') dump xmltree $apk AndroidManifest.xml
if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect manifest components' }
if (($xml -join "`n") -match 'nativeapp\.audit\.') { throw 'Debug-only test components leaked into beta' }
$mapping = Join-Path $repo 'app\build\outputs\mapping\beta\mapping.txt'
if (!(Test-Path -LiteralPath $mapping) -or (Get-Item -LiteralPath $mapping).Length -eq 0) { throw 'Missing R8 mapping evidence' }
$record = [ordered]@{
    checkedAt = (Get-Date).ToString('o'); variant = 'beta'; package = $package.Groups[1].Value
    versionCode = [long]$package.Groups[2].Value; versionName = $package.Groups[3].Value
    debuggable = $false; debugComponentsAbsent = $true; certificateSha256 = $certificate
    apk = $apk; bytes = (Get-Item -LiteralPath $apk).Length; sha256 = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant()
    mappingSha256 = (Get-FileHash -LiteralPath $mapping -Algorithm SHA256).Hash.ToLowerInvariant()
    gitHead = (& git -C $repo rev-parse HEAD).Trim(); dirty = [bool](& git -C $repo status --porcelain)
    artifactChecksPassed = $true; runtimeVerified = $false; readyToRelease = $false
}
$evidence = Join-Path 'D:\NovalPie\agent-bridge\artifacts\beta7-device' "$EvidenceName.json"
$record | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath $evidence -Encoding utf8
$record | ConvertTo-Json -Depth 4 -Compress
