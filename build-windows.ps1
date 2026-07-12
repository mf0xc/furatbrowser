# Furat Browser - Windows Build Script
# Run this in PowerShell as Administrator

Write-Host "=========================================" -ForegroundColor Cyan
Write-Host "   Furat Browser - Windows Builder" -ForegroundColor Cyan
Write-Host "=========================================" -ForegroundColor Cyan
Write-Host ""

# Check Java
Write-Host "[1/5] Checking Java..." -ForegroundColor Yellow
$javaVersion = java -version 2>&1 | Select-String -Pattern '"([0-9\.]+)"' | ForEach-Object { $_.Matches.Groups[1].Value }
if ($javaVersion) {
    Write-Host "      ✓ Java found: $javaVersion" -ForegroundColor Green
} else {
    Write-Host "      ✗ Java not found!" -ForegroundColor Red
    Write-Host "      Please install JDK 17 from: https://adoptium.net/" -ForegroundColor Red
    exit 1
}

# Check Android SDK
Write-Host "[2/5] Checking Android SDK..." -ForegroundColor Yellow
$androidHome = $env:ANDROID_HOME
if (-not $androidHome) {
    $androidHome = $env:ANDROID_SDK_ROOT
}
if ($androidHome -and (Test-Path "$androidHome\platform-tools")) {
    Write-Host "      ✓ Android SDK found: $androidHome" -ForegroundColor Green
} else {
    Write-Host "      ⚠ Android SDK not found in environment" -ForegroundColor Yellow
    Write-Host "      You may need Android Studio installed" -ForegroundColor Yellow
}

# Download Gradle Wrapper if missing
Write-Host "[3/5] Checking Gradle Wrapper..." -ForegroundColor Yellow
$wrapperJar = "gradle\wrapper\gradle-wrapper.jar"
if (-not (Test-Path $wrapperJar)) {
    Write-Host "      ⚠ gradle-wrapper.jar missing" -ForegroundColor Yellow
    Write-Host "      Downloading..." -ForegroundColor Yellow

    # Create gradle wrapper directory
    New-Item -ItemType Directory -Force -Path "gradle\wrapper" | Out-Null

    # Download gradle-wrapper.jar from Gradle's CDN
    $wrapperUrl = "https://raw.githubusercontent.com/gradle/gradle/v8.4.0/gradle/wrapper/gradle-wrapper.jar"
    try {
        Invoke-WebRequest -Uri $wrapperUrl -OutFile $wrapperJar -UseBasicParsing
        Write-Host "      ✓ Downloaded gradle-wrapper.jar" -ForegroundColor Green
    } catch {
        Write-Host "      ✗ Failed to download wrapper" -ForegroundColor Red
        Write-Host "      Please use Android Studio or download Gradle manually" -ForegroundColor Red
        exit 1
    }
} else {
    Write-Host "      ✓ Gradle wrapper found" -ForegroundColor Green
}

# Build Debug APK
Write-Host "[4/5] Building Debug APK..." -ForegroundColor Yellow
Write-Host "      This may take 5-10 minutes on first run..." -ForegroundColor Gray

$env:JAVA_OPTS = "-Xmx2048m"
$env:GRADLE_OPTS = "-Dorg.gradle.jvmargs=-Xmx2048m"

& .\gradlew.bat assembleDebug --no-daemon

if ($LASTEXITCODE -eq 0) {
    Write-Host "      ✓ Build successful!" -ForegroundColor Green
} else {
    Write-Host "      ✗ Build failed!" -ForegroundColor Red
    exit 1
}

# Show output
Write-Host "[5/5] Build complete!" -ForegroundColor Green
Write-Host ""
Write-Host "APK Location:" -ForegroundColor Cyan
Write-Host "  Debug: app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White
Write-Host ""
Write-Host "Install on device:" -ForegroundColor Cyan
Write-Host "  adb install app\build\outputs\apk\debug\app-debug.apk" -ForegroundColor White
Write-Host ""
Write-Host "=========================================" -ForegroundColor Cyan
