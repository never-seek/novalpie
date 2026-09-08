param(
    [Parameter(Mandatory)][string]$Report,
    [Parameter(Mandatory)][string]$EvidenceName,
    [string]$Adb = 'C:\Users\86188\AppData\Local\Android\Sdk\platform-tools\adb.exe',
    [string]$Serial = '127.0.0.1:16384'
)
$ErrorActionPreference = 'Stop'
# Only instrumented QA reports, never session stores or task authorization records.
if ($Report -notmatch '^cache/beta7-[a-zA-Z0-9_-]+/[a-zA-Z0-9_-]+\.json$') { throw 'Expected a Beta7 test report' }
if ($EvidenceName -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Use a simple evidence name' }
$lines = & $Adb -s $Serial shell run-as com.novalpie.app.debug cat $Report
if ($LASTEXITCODE -ne 0) { throw 'Could not collect the device report' }
$raw = $lines -join "`n"
$data = $raw | ConvertFrom-Json
$root = 'D:\NovalPie\agent-bridge\artifacts\beta7-device'
New-Item -ItemType Directory -Force -Path $root | Out-Null
$target = Join-Path $root "$EvidenceName.json"
[System.IO.File]::WriteAllText($target, $raw, [System.Text.UTF8Encoding]::new($false))
[pscustomobject]@{report=$target;bytes=(Get-Item -LiteralPath $target).Length;sha256=(Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash;passed=$data.passed} | ConvertTo-Json -Compress
