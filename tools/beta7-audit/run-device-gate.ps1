param(
    [string]$Adb = 'C:\Users\86188\AppData\Local\Android\Sdk\platform-tools\adb.exe',
    [string]$Serial = '127.0.0.1:16384',
    [string]$TestClass = 'com.novalpie.nativeapp.ui.ReaderTtsDeviceTest',
    [string]$EvidenceName = 'tts-device',
    [long]$BookId = 0,
    [long]$ChapterId = 0,
    [switch]$AllowPublicRuleWrite,
    [switch]$VerifyImageArchive,
    [switch]$VerifyFullImagePrecheck,
    [switch]$AllowForumWrite
)
$ErrorActionPreference = 'Stop'
if ($TestClass -notmatch '^com\.novalpie\.nativeapp\.[A-Za-z0-9_.]+$') { throw 'Only NovalPie test classes are accepted' }
if ($EvidenceName -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Use a simple evidence name' }
if ($BookId -lt 0 -or $ChapterId -lt 0) { throw 'Source IDs must be positive when supplied' }
$repositoryRoot = [System.IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..\..'))
$appApk = Join-Path $repositoryRoot 'app\build\outputs\apk\debug\app-debug.apk'
$testApk = Join-Path $repositoryRoot 'app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk'
$outputRoot = 'D:\NovalPie\agent-bridge\artifacts\beta7-device'
New-Item -ItemType Directory -Force -Path $outputRoot | Out-Null
$installedPath = (& $Adb -s $Serial shell pm path com.novalpie.app.debug | Select-Object -First 1) -replace '^package:', ''
if ($installedPath -notmatch '^/data/app/.+/base\.apk$') { throw 'Cannot identify the installed NovalPie APK' }
$installedSha = ((& $Adb -s $Serial shell sha256sum $installedPath) -split '\s+')[0].ToLowerInvariant()
$localSha = (Get-FileHash -LiteralPath $appApk -Algorithm SHA256).Hash.ToLowerInvariant()
if ($installedSha -ne $localSha) { throw 'Installed app differs from local build. Install deliberately before running this gate.' }
$metadata = [ordered]@{
    capturedAt = (Get-Date).ToString('o')
    testClass = $TestClass
    package = 'com.novalpie.app.debug'
    serial = $Serial
    sdk = (& $Adb -s $Serial shell getprop ro.build.version.sdk).Trim()
    size = (& $Adb -s $Serial shell wm size) -join '; '
    density = (& $Adb -s $Serial shell wm density) -join '; '
    fontScale = (& $Adb -s $Serial shell settings get system font_scale).Trim()
    inputDisplayOrientation = ((& $Adb -s $Serial shell dumpsys input) | Select-String 'SurfaceOrientation|SurfaceWidth|SurfaceHeight') -join '; '
    apkSha256 = $localSha
    testApkSha256 = (Get-FileHash -LiteralPath $testApk -Algorithm SHA256).Hash.ToLowerInvariant()
    gitHead = (& git -C $repositoryRoot rev-parse HEAD).Trim()
    dirty = [bool](& git -C $repositoryRoot status --porcelain)
}
$instrumentArgs = @('-s', $Serial, 'shell', 'am', 'instrument', '-w', '-r', '-e', 'class', $TestClass)
if ($BookId -gt 0) { $instrumentArgs += @('-e', 'bookId', $BookId.ToString()); $metadata.bookId = $BookId }
if ($ChapterId -gt 0) { $instrumentArgs += @('-e', 'chapterId', $ChapterId.ToString()); $metadata.chapterId = $ChapterId }
if ($AllowPublicRuleWrite) { $instrumentArgs += @('-e', 'allowPublicRuleWrite', 'true'); $metadata.publicRuleWrite = $true }
if ($VerifyImageArchive) { $instrumentArgs += @('-e', 'verifyImageArchive', 'true'); $metadata.verifyImageArchive = $true }
if ($VerifyFullImagePrecheck) { $instrumentArgs += @('-e', 'verifyFullImagePrecheck', 'true'); $metadata.verifyFullImagePrecheck = $true }
if ($AllowForumWrite) { $instrumentArgs += @('-e', 'allowForumWrite', 'true'); $metadata.allowForumWrite = $true }
$instrumentArgs += 'com.novalpie.app.debug.test/androidx.test.runner.AndroidJUnitRunner'
$log = & $Adb @instrumentArgs 2>&1
$logText = $log -join "`n"
$metadata.passed = $logText -match 'OK \(\d+ tests?\)' -and $logText -notmatch 'FAILURES!!!|INSTRUMENTATION_FAILED|Process crashed'
$logFile = Join-Path $outputRoot "$EvidenceName.log"
$metadataFile = Join-Path $outputRoot "$EvidenceName.json"
$logText | Set-Content -LiteralPath $logFile -Encoding utf8
$metadata | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath $metadataFile -Encoding utf8
[pscustomobject]@{passed=$metadata.passed;metadata=$metadataFile;log=$logFile;apk=$localSha} | ConvertTo-Json -Compress
# am instrument often returns shell exit 0 even when every test fails. Parse the JUnit result.
if (!$metadata.passed) { throw "Device regression failed; inspect $logFile" }
