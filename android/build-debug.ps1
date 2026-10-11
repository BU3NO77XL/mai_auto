param(
    [string]$SdkPath = $(if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { $env:ANDROID_SDK_ROOT }),
    [string]$GradlePath = 'gradle.bat',
    [switch]$Install
)

$ErrorActionPreference = 'Stop'
$projectPath = Split-Path -Parent $MyInvocation.MyCommand.Path
$apkPath = Join-Path $projectPath 'app\build\outputs\apk\debug\app-debug.apk'

if (-not $SdkPath -or -not (Test-Path -LiteralPath (Join-Path $SdkPath 'platforms\android-35\android.jar'))) {
    throw 'Android SDK 35 não encontrado. Informe -SdkPath com o caminho do SDK.'
}
$env:ANDROID_HOME = $SdkPath
$env:ANDROID_SDK_ROOT = $SdkPath

if (-not (Get-Command $GradlePath -ErrorAction SilentlyContinue)) {
    throw 'Gradle não encontrado. Informe -GradlePath com o caminho de gradle.bat (Gradle 8.11.1).'
}

Push-Location $projectPath
try {
    & $GradlePath ':app:assembleDebug' '--no-daemon'
    if ($LASTEXITCODE -ne 0) { throw "Build falhou com código $LASTEXITCODE" }
} finally {
    Pop-Location
}
if (-not (Test-Path -LiteralPath $apkPath)) { throw "Build terminou sem APK em $apkPath" }
Write-Output "APK: $apkPath"

if ($Install) {
    $adbPath = Join-Path $SdkPath 'platform-tools\adb.exe'
    if (-not (Test-Path -LiteralPath $adbPath)) { throw 'ADB não encontrado no SDK informado.' }
    $devices = @(& $adbPath devices | Where-Object { $_ -match '^\S+\s+device$' })
    if ($devices.Count -ne 1) { throw "É necessário exatamente um dispositivo USB autorizado; encontrados: $($devices.Count)." }
    & $adbPath install -r $apkPath
    if ($LASTEXITCODE -ne 0) { throw "Instalação falhou com código $LASTEXITCODE" }
    Write-Output 'Hydra instalado no dispositivo.'
}
