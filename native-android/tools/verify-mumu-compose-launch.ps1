param(
    [string]$AdbPath = "C:\Users\86188\AppData\Local\Android\Sdk\platform-tools\adb.exe",
    [string]$MuMuManagerPath = "D:\Program Files\MuMu\emulator\MuMuPlayer-12.0\nx_main\MuMuManager.exe",
    [int]$VmIndex = 0,
    [string]$ApkPath = "D:\NovalPie\NovalPie-native-2.0-release.apk",
    [string]$EvidenceRoot = "D:\NovalPie\smoke-results",
    [string]$PackageName = "com.novalpie.app",
    [string]$ActivityName = "com.novalpie.nativeapp.MainActivity",
    [string]$Marker = "NOVALPIE_NATIVE_COMPOSE_HOME",
    [switch]$SkipInstall,
    [switch]$SkipProxyReverse
)

$ErrorActionPreference = "Stop"

function Save-Text {
    param(
        [string]$Path,
        [string]$Text
    )
    $Text | Set-Content -LiteralPath $Path -Encoding UTF8
}

function Invoke-TextCommand {
    param(
        [string]$OutputPath,
        [scriptblock]$Command
    )
    try {
        $text = (& $Command 2>&1 | Out-String)
        Save-Text -Path $OutputPath -Text $text
        return $text
    } catch {
        $text = $_ | Out-String
        Save-Text -Path $OutputPath -Text $text
        return $text
    }
}

function Get-FirstAdbDevice {
    param([string]$DevicesText)
    $lines = $DevicesText -split "`r?`n"
    foreach ($line in $lines) {
        $trimmed = $line.Trim()
        if ($trimmed -eq "" -or $trimmed.StartsWith("List of devices")) {
            continue
        }
        $parts = $trimmed -split "\s+"
        if ($parts.Length -ge 2 -and $parts[1] -eq "device") {
            return $parts[0]
        }
    }
    return $null
}

function Write-Summary {
    param(
        [string]$EvidenceDir,
        [string]$Status,
        [string]$Serial,
        [bool]$MarkerFound,
        [string]$Reason
    )
    $hash = $null
    if (Test-Path -LiteralPath $ApkPath) {
        $hash = (Get-FileHash -Algorithm SHA256 -LiteralPath $ApkPath).Hash
    }
    [ordered]@{
        status = $Status
        serial = $Serial
        package = $PackageName
        activity = "$PackageName/$ActivityName"
        apk = $ApkPath
        sha256 = $hash
        marker = $Marker
        marker_found = $MarkerFound
        reason = $Reason
        evidence_dir = $EvidenceDir
    } | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $EvidenceDir "summary.json") -Encoding UTF8
}

if (-not (Test-Path -LiteralPath $AdbPath)) {
    throw "Missing adb: $AdbPath"
}
if (-not (Test-Path -LiteralPath $ApkPath)) {
    throw "Missing APK: $ApkPath"
}

$timestamp = Get-Date -Format "yyyyMMdd-HHmmss"
$evidenceDir = Join-Path $EvidenceRoot "native-android-mumu-compose-launch-$timestamp"
New-Item -ItemType Directory -Force -Path $evidenceDir | Out-Null

if (Test-Path -LiteralPath $MuMuManagerPath) {
    Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "mumu-info-before.json") -Command {
        & $MuMuManagerPath info --vmindex $VmIndex
    } | Out-Null
    Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "mumu-adb-connect.txt") -Command {
        & $MuMuManagerPath adb --vmindex $VmIndex --cmd connect
    } | Out-Null
}

$devicesText = Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "adb-devices-before.txt") -Command {
    & $AdbPath devices -l
}
$serial = Get-FirstAdbDevice -DevicesText $devicesText

if ($null -eq $serial) {
    Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "adb-connect-localhost-5555.txt") -Command {
        & $AdbPath connect 127.0.0.1:5555
    } | Out-Null
    $devicesText = Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "adb-devices-after-localhost.txt") -Command {
        & $AdbPath devices -l
    }
    $serial = Get-FirstAdbDevice -DevicesText $devicesText
}

if ($null -eq $serial) {
    if (Test-Path -LiteralPath $MuMuManagerPath) {
        Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "mumu-info-after.json") -Command {
            & $MuMuManagerPath info --vmindex $VmIndex
        } | Out-Null
    }
    Write-Summary -EvidenceDir $evidenceDir -Status "blocked-no-adb-device" -Serial "" -MarkerFound $false -Reason "No adb device is online. MuMu may still be booting or stuck before Android starts."
    Write-Host "Blocked: no adb device. Evidence: $evidenceDir"
    exit 2
}

Save-Text -Path (Join-Path $evidenceDir "selected-serial.txt") -Text $serial

if (-not $SkipProxyReverse) {
    Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "adb-reverse-7890.txt") -Command {
        & $AdbPath -s $serial reverse tcp:7890 tcp:7890
    } | Out-Null
}

if (-not $SkipInstall) {
    Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "adb-install.txt") -Command {
        & $AdbPath -s $serial install -r $ApkPath
    } | Out-Null
}

Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "resolve-activity.txt") -Command {
    & $AdbPath -s $serial shell cmd package resolve-activity --brief $PackageName
} | Out-Null

Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "am-start.txt") -Command {
    & $AdbPath -s $serial shell am start -n "$PackageName/$ActivityName"
} | Out-Null

Start-Sleep -Seconds 6

$uiText = Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "ui-tree.xml") -Command {
    & $AdbPath -s $serial exec-out uiautomator dump /dev/tty
}

$screenshotPath = Join-Path $evidenceDir "screenshot.png"
$screenshotCommand = "`"$AdbPath`" -s $serial exec-out screencap -p > `"$screenshotPath`""
cmd.exe /c $screenshotCommand | Out-Null

Invoke-TextCommand -OutputPath (Join-Path $evidenceDir "logcat-crash.txt") -Command {
    & $AdbPath -s $serial logcat -b crash -d
} | Out-Null

$markerFound = $uiText.Contains($Marker)
if ($markerFound) {
    Write-Summary -EvidenceDir $evidenceDir -Status "pass" -Serial $serial -MarkerFound $true -Reason "Marker found in UIAutomator dump."
    Write-Host "PASS: Compose marker found. Evidence: $evidenceDir"
    exit 0
}

Write-Summary -EvidenceDir $evidenceDir -Status "fail-marker-missing" -Serial $serial -MarkerFound $false -Reason "App launched but marker was not found in UIAutomator dump."
Write-Host "FAIL: marker not found. Evidence: $evidenceDir"
exit 1
