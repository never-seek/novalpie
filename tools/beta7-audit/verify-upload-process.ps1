param(
    [string]$Adb = 'C:\Users\86188\AppData\Local\Android\Sdk\platform-tools\adb.exe',
    [string]$Serial = '127.0.0.1:16384',
    [string]$EvidenceName = 'upload-process-recovery'
)
$ErrorActionPreference = 'Stop'
if ($EvidenceName -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Use a simple evidence name' }
function Assert-NoBackgroundService {
    $services = & $Adb -s $Serial shell dumpsys activity services com.novalpie.app.debug
    if ($LASTEXITCODE -ne 0) { throw 'Could not inspect current NovalPie services' }
    if (($services -join "`n") -match 'ServiceRecord\{') { throw 'NovalPie has a running service; do not force-stop active user work' }
}
Assert-NoBackgroundService
$processCaseId = [Guid]::NewGuid().ToString('N')
& (Join-Path $PSScriptRoot 'run-device-gate.ps1') -Adb $Adb -Serial $Serial -TestClass com.novalpie.nativeapp.ui.UploadDraftProcessPrepareTest -ProcessCaseId $processCaseId -EvidenceName "$EvidenceName-prepare"
Assert-NoBackgroundService
& $Adb -s $Serial shell am force-stop com.novalpie.app.debug
if ($LASTEXITCODE -ne 0) { throw 'Could not force-stop the scoped NovalPie test process' }
$remainingPid = & $Adb -s $Serial shell pidof com.novalpie.app.debug
if ($remainingPid) { throw 'The old NovalPie process is still running' }
& (Join-Path $PSScriptRoot 'run-device-gate.ps1') -Adb $Adb -Serial $Serial -TestClass com.novalpie.nativeapp.ui.UploadDraftProcessVerifyTest -ProcessCaseId $processCaseId -EvidenceName "$EvidenceName-verify"
& (Join-Path $PSScriptRoot 'collect-device-report.ps1') -Adb $Adb -Serial $Serial -Report 'cache/beta7-upload-process-report/process.json' -EvidenceName "$EvidenceName-report"
[pscustomobject]@{case=$processCaseId;forceStopVerified=$true;oldProcessAbsent=$true;result='verified by separate process test';evidence=$EvidenceName} | ConvertTo-Json -Compress
