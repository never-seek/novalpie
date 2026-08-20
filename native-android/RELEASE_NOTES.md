# NovalPie Native Android 2.0.0-beta2

This branch publishes the current native Android client for testing and community distribution.

## Included

- Native Kotlin/Jetpack Compose catalogue, search, bookshelf, forum, profile, and book detail flows.
- Native reader with continuous chapter loading, chapter catalogue, comments, settings, images, and
  persisted reading progress.
- Correct bookshelf progress semantics: an unread book starts at `0/N`; local chapter progress is
  retained; a refreshed source total can show `130/131` and an update marker.
- Native TXT/EPUB download paths, cover/original-image preview handling, proxy configuration, and
  source-compatible API normalisation.
- Unit tests and lint coverage for the current app behavior.

## Verification

The source used for this release was verified with:

```powershell
.\gradlew.bat :app:testReleaseUnitTest --rerun-tasks --offline --no-daemon --max-workers=1 --console=plain
.\gradlew.bat :app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain
```

The final verification produced 80 test suites and 566 tests with zero failures, errors, or
skips; lint reported `No issues found`.

## APK

The GitHub Release asset is a debug-signed APK built from this branch. It is suitable for direct
Android installation after enabling installation from the chosen file source. A future production
release should use a project-controlled release keystore and a signed `assembleRelease` artifact.

- Asset: `NovalPie-native-2.0.0-beta2-debug.apk`
- SHA-256: `627D88A3F5C1C415539785C82A0F9F780084AFA70967FC6800534FB61C27B4E2`

Before installing an update, uninstall an older package only if Android reports a signing conflict;
uninstalling may remove that app's local session and cached reading data.
