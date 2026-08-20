# NovalPie

NovalPie is a NovelPia community client project. The existing web/tooling project remains in the
repository root, and the current native Android 2.0 client is available under
[`native-android/`](native-android/).

## Native Android 2.0

The native client is a Kotlin + Jetpack Compose application with native catalogue/search, forum,
bookshelf, book details, reader, downloads, profile, and administration surfaces. WebView is only
used as a fallback for source flows that are not yet native.

Build it on Windows with JDK 17 and Android SDK platform 35:

```powershell
cd native-android
.\gradlew.bat :app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain
```

Run the tests and lint checks:

```powershell
.\gradlew.bat :app:testReleaseUnitTest :app:lintDebug --offline --no-daemon --max-workers=1 --console=plain
```

The latest installable debug APK is published in the [Releases](https://github.com/never-seek/novalpie/releases)
section. See [`native-android/RELEASE_NOTES.md`](native-android/RELEASE_NOTES.md) for the release
scope, checksum, and installation notes.

## Existing project

The original project files and scripts remain available in `src/`, `novalpie-app/`, `scripts/`,
and the existing root documentation. The native client is intentionally kept in its own Gradle
module so the two toolchains do not overwrite each other's build state.
