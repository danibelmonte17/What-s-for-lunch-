$ErrorActionPreference = "Continue"

$Root = $PSScriptRoot
Set-Location $Root

# JAVA_HOME: usa el JBR de Android Studio si no esta definido
if (-not $env:JAVA_HOME) {
    foreach ($jbr in @("$env:LOCALAPPDATA\Programs\Android Studio\jbr", "C:\Program Files\Android\Android Studio\jbr", "$env:USERPROFILE\android-studio\jbr")) {
        if (Test-Path $jbr) { $env:JAVA_HOME = $jbr; break }
    }
}

if (-not (Test-Path (Join-Path $Root "gradlew"))) {
    Write-Error "no hay proyecto Android todavia (falta gradlew)."
    exit 1
}

Write-Host "==> Lint"
& .\gradlew.bat lintDebug
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
