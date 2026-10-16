$ErrorActionPreference = "Stop"

# Rutas absolutas resueltas desde el propio script
$Root = $PSScriptRoot
Set-Location $Root
New-Item -ItemType Directory -Force -Path (Join-Path $Root ".harness") | Out-Null

# JAVA_HOME: usa el JBR de Android Studio si no esta definido
if (-not $env:JAVA_HOME) {
    foreach ($jbr in @("$env:LOCALAPPDATA\Programs\Android Studio\jbr", "C:\Program Files\Android\Android Studio\jbr", "$env:USERPROFILE\android-studio\jbr")) {
        if (Test-Path $jbr) { $env:JAVA_HOME = $jbr; break }
    }
}

if (-not (Test-Path (Join-Path $Root "gradlew"))) {
    Write-Error "no hay proyecto Android todavia (falta gradlew). Ejecuta init cuando exista el scaffold."
    exit 1
}
if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    Write-Error "adb no esta en el PATH."
    exit 1
}

$SdkDir = $env:ANDROID_HOME
if (-not $SdkDir) { $SdkDir = $env:ANDROID_SDK_ROOT }
if (-not $SdkDir) {
    $lp = Join-Path $Root "local.properties"
    if (Test-Path $lp) {
        $m = Select-String -Path $lp -Pattern "^sdk\.dir=" | Select-Object -First 1
        if ($m) { $SdkDir = ($m.Line -replace "^sdk\.dir=", "").Trim() }
    }
}

function Invoke-Step {
    param([string]$Cmd, [string[]]$CmdArgs = @())
    & $Cmd @CmdArgs
    if ($LASTEXITCODE -ne 0) { throw "Fallo: $Cmd $($CmdArgs -join ' ')" }
}

function Invoke-Capture {
    param([string]$Cmd, [string[]]$CmdArgs = @())
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    $out = & $Cmd @CmdArgs 2>$null | Out-String
    $code = $LASTEXITCODE
    $ErrorActionPreference = $prev
    return [PSCustomObject]@{ Code = $code; Out = $out.Trim() }
}

Write-Host "==> Instalando dependencias"
Invoke-Step ".\gradlew.bat" @("--version")

Write-Host "==> Preparando entorno"
if ($SdkDir -and -not (Test-Path (Join-Path $Root "local.properties"))) {
    Set-Content -Path (Join-Path $Root "local.properties") -Value ("sdk.dir=" + ($SdkDir -replace "\\", "/"))
}
$state = Invoke-Capture "adb" @("get-state")
if ($state.Code -ne 0) {
    Write-Error "no hay emulador ni dispositivo conectado (adb get-state)."
    exit 1
}

Write-Host "==> Arrancando la app"
Invoke-Step ".\gradlew.bat" @("installDebug")

$apk = Get-ChildItem -Path (Join-Path $Root "app\build\outputs\apk\debug") -Filter "*.apk" -ErrorAction SilentlyContinue | Select-Object -First 1
if (-not $apk) {
    Write-Error "no se genero el APK en app\build\outputs\apk\debug."
    exit 1
}

# El identificador del paquete se lee del APK generado; se puede forzar con la variable de entorno PKG
$pkg = $env:PKG
if (-not $pkg) {
    $aapt = Get-Command aapt -ErrorAction SilentlyContinue
    if ($aapt) {
        $r = Invoke-Capture $aapt.Source @("dump", "badging", $apk.FullName)
        if ($r.Out -match "package: name='([^']+)'") { $pkg = $Matches[1] }
    }
    if (-not $pkg -and $SdkDir) {
        $aapt2 = Get-ChildItem -Path (Join-Path $SdkDir "build-tools") -Recurse -Filter "aapt2.exe" -ErrorAction SilentlyContinue | Select-Object -Last 1
        if ($aapt2) {
            $r = Invoke-Capture $aapt2.FullName @("dump", "badging", $apk.FullName)
            if ($r.Out -match "package: name='([^']+)'") { $pkg = $Matches[1] }
        }
    }
}
if (-not $pkg) {
    Write-Error "no se pudo determinar el paquete de la app. Define la variable de entorno PKG con su applicationId y vuelve a ejecutar."
    exit 1
}

Invoke-Step "adb" @("shell", "monkey", "-p", $pkg, "-c", "android.intent.category.LAUNCHER", "1")

Write-Host "==> Verificando"
$pathCheck = Invoke-Capture "adb" @("shell", "pm", "path", $pkg)
if ($pathCheck.Code -ne 0) {
    Write-Error "la app no quedo instalada en el dispositivo."
    exit 1
}
$proc = Invoke-Capture "adb" @("shell", "pidof", $pkg)
if (-not $proc.Out) {
    Write-Error "la app no esta en ejecucion tras el arranque."
    exit 1
}

Write-Host "==> Entorno listo"
Write-Host "App $pkg en ejecucion sobre el dispositivo de adb"
exit 0
