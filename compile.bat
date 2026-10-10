@echo off
setlocal

cd /d "%~dp0"
if errorlevel 1 exit /b 1

set "JAVA_HOME=C:\Program Files\Eclipse Adoptium\jdk-21.0.12.101-hotspot"
set "ANDROID_HOME=%LOCALAPPDATA%\Android\Sdk"

call ".\gradlew.bat" -g "%USERPROFILE%\.gradle" --refresh-dependencies :patches:buildVibePatch :patches:test "-PyanosikApk=%CD%\apk\yanosik_antisplit.apk"
set "BUILD_EXIT=%ERRORLEVEL%"

if "%BUILD_EXIT%"=="0" (
    for /f "tokens=2 delims== " %%V in ('findstr /b "version" gradle.properties') do echo Patch bundle: "%CD%\patches\build\libs\vibe-patch-%%V.mpp"
) else (
    echo Build failed with exit code %BUILD_EXIT%.
)

exit /b %BUILD_EXIT%
