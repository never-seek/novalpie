param(
    [string]$ProjectDir = "D:\NovalPie\native-android",
    [string]$OutputApk = "D:\NovalPie\NovalPie-native-2.0-release.apk",
    [string]$SigningDir = "D:\NovalPie\commercial-app\signing",
    [string]$BuildTools = "C:\Users\86188\AppData\Local\Android\Sdk\build-tools\34.0.0"
)

$ErrorActionPreference = "Stop"

function Read-SigningCredentials {
    param([Parameter(Mandatory)][string]$Path)
    if (!(Test-Path -LiteralPath $Path)) {
        throw "Signing credentials not found: $Path"
    }

    $values = @{}
    foreach ($line in Get-Content -LiteralPath $Path) {
        $trimmed = $line.Trim()
        if (!$trimmed -or $trimmed.StartsWith("#")) {
            continue
        }
        $parts = $trimmed -split "=", 2
        if ($parts.Count -eq 2) {
            $values[$parts[0].Trim()] = $parts[1].Trim()
        }
    }
    return $values
}

function Get-CredentialValue {
    param(
        [hashtable]$Values,
        [string[]]$Keys
    )
    foreach ($key in $Keys) {
        if ($Values.ContainsKey($key) -and $Values[$key]) {
            return $Values[$key]
        }
    }
    return $null
}

function Invoke-Checked {
    param(
        [string]$Name,
        [scriptblock]$Action
    )
    & $Action
    if ($LASTEXITCODE -ne 0) {
        throw "$Name failed with exit code $LASTEXITCODE"
    }
}

$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"

$unsignedApk = Join-Path $ProjectDir "app\build\outputs\apk\release\app-release-unsigned.apk"
$alignedApk = Join-Path $ProjectDir "app\build\outputs\apk\release\app-release-aligned.apk"
$signedApk = Join-Path $ProjectDir "app\build\outputs\apk\release\app-release.apk"
$keystore = Join-Path $SigningDir "novalpie-release.jks"
$credentialsPath = Join-Path $SigningDir "keystore-credentials.txt"
$zipalign = Join-Path $BuildTools "zipalign.exe"
$apksigner = Join-Path $BuildTools "apksigner.bat"

foreach ($requiredPath in @($ProjectDir, $keystore, $credentialsPath, $zipalign, $apksigner)) {
    if (!(Test-Path -LiteralPath $requiredPath)) {
        throw "Required path missing: $requiredPath"
    }
}

Push-Location $ProjectDir
try {
    Invoke-Checked "Gradle assembleRelease" { .\gradlew.bat :app:assembleRelease --console=plain --no-daemon }
}
finally {
    Pop-Location
}

if (!(Test-Path -LiteralPath $unsignedApk)) {
    throw "Unsigned APK not found: $unsignedApk"
}

$credentials = Read-SigningCredentials $credentialsPath
$storePass = Get-CredentialValue $credentials @("storePassword", "store_pass", "STORE_PASS", "ksPass", "password")
$keyPass = Get-CredentialValue $credentials @("keyPassword", "key_pass", "KEY_PASS", "ksPass", "password")
$keyAlias = Get-CredentialValue $credentials @("keyAlias", "alias", "KEY_ALIAS")

if (!$storePass) {
    throw "Missing signing store password in $credentialsPath"
}
if (!$keyPass) {
    $keyPass = $storePass
}
if (!$keyAlias) {
    $keyAlias = "novalpie"
}

Invoke-Checked "zipalign" { & $zipalign -p -f 4 $unsignedApk $alignedApk }
Invoke-Checked "apksigner sign" {
    & $apksigner -JXmx128M sign `
        --ks $keystore `
        --ks-key-alias $keyAlias `
        --ks-pass "pass:$storePass" `
        --key-pass "pass:$keyPass" `
        --out $signedApk `
        $alignedApk
}
Invoke-Checked "apksigner verify" { & $apksigner -JXmx128M verify --verbose $signedApk }

Copy-Item -LiteralPath $signedApk -Destination $OutputApk -Force

[pscustomobject]@{
    SignedApk = $signedApk
    OutputApk = $OutputApk
    Sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $OutputApk).Hash
} | ConvertTo-Json -Depth 3
