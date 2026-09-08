param(
    [Parameter(Mandatory)][string]$Name,
    [string]$Adb = 'C:\Users\86188\AppData\Local\Android\Sdk\platform-tools\adb.exe',
    [string]$Serial = '127.0.0.1:16384'
)
$ErrorActionPreference = 'Stop'
if ($Name -notmatch '^[a-zA-Z0-9_-]+$') { throw 'Use a simple evidence name' }
$target = Join-Path 'D:\NovalPie\agent-bridge\screenshots' "$Name.png"
$start = [System.Diagnostics.ProcessStartInfo]::new()
$start.FileName = $Adb
$start.UseShellExecute = $false
$start.CreateNoWindow = $true
$start.RedirectStandardOutput = $true
$start.RedirectStandardError = $true
foreach ($part in @('-s', $Serial, 'exec-out', 'screencap', '-p')) { $start.ArgumentList.Add($part) }
$process = [System.Diagnostics.Process]::Start($start)
try {
    $errorRead = $process.StandardError.ReadToEndAsync()
    $destination = [System.IO.File]::Create($target)
    try { $process.StandardOutput.BaseStream.CopyTo($destination) } finally { $destination.Dispose() }
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) { throw "Screenshot failed: $($errorRead.GetAwaiter().GetResult())" }
    if ((Get-Item -LiteralPath $target).Length -lt 100) { throw 'Screenshot response was empty' }
    [pscustomobject]@{path=$target;bytes=(Get-Item -LiteralPath $target).Length} | ConvertTo-Json -Compress
} finally { $process.Dispose() }
