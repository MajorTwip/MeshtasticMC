@echo off
set JAVA_HOME=C:\Program Files\Zulu\zulu-21
set PATH=%JAVA_HOME%\bin;C:\Android\Sdk\platform-tools;%PATH%

cd /d %~dp0
call gradlew.bat installDebug || exit /b 1
adb shell am start -n com.majortwip.meshtasticmc/.MainActivity
