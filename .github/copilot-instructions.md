# Copilot Agent Instructions

## Building & Installing

Always build with **JDK 21** (`C:\Program Files\Zulu\zulu-21`). The default JDK on this machine is JDK 25, which causes the Kotlin Gradle plugin to crash with `IllegalArgumentException: 25.0.2` when parsing the Java version.

Set `JAVA_HOME` before invoking Gradle:

```powershell
$env:JAVA_HOME = "C:\Program Files\Zulu\zulu-21"
$env:PATH = "C:\Program Files\Zulu\zulu-21\bin;" + ($env:PATH -replace "C:\\Program Files\\Zulu\\zulu-25\\bin;","")
.\gradlew installDebug
```

If Gradle daemons from a previous JDK-25 session are cached, stop them first:

```powershell
.\gradlew --stop
```

## Meshtastic Proto API (v2.7.13)

The `org.meshtastic.proto.Position` class exposes **snake_case** field names (Wire protobuf codegen), not camelCase:

- `position.latitude_i` (not `latitudeI`)
- `position.longitude_i` (not `longitudeI`)
- `position.sats_in_view` (not `satsInView`)

## ADB

ADB is at `C:\Android\Sdk\platform-tools\adb.exe`. Add to PATH for the session:

```powershell
$env:PATH += ";C:\Android\Sdk\platform-tools"
```

Launch the app:

```powershell
adb shell am start -n com.majortwip.meshtasticmc/.MainActivity
```

Watch the log:

```powershell
adb logcat --pid=$(adb shell pidof com.majortwip.meshtasticmc) -v time
```

## Known Warnings (non-fatal)

- AGP 8.7.3 was tested up to `compileSdk = 35`; the project uses `compileSdk = 36`. Build still succeeds.
- D8 emits "Unexpected error during rewriting of Kotlin metadata" for Kotlin 2.3.0 classes. APK installs and runs correctly.
