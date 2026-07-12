@echo off
echo =========================================
echo    Furat Browser - Windows Builder
echo =========================================
echo.
echo [1/3] Building with Gradle...
.
gradlew.bat assembleDebug --no-daemon
if %ERRORLEVEL% NEQ 0 (
    echo.
    echo BUILD FAILED!
    pause
    exit /b 1
)
echo.
echo [2/3] Build successful!
echo.
echo [3/3] APK Location:
echo   appuild\outputspk\debugpp-debug.apk
echo.
echo Install with: adb install appuild\outputspk\debugpp-debug.apk
echo.
pause
