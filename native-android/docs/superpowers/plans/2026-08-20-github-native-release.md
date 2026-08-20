# NovalPie Native Android GitHub Release Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Publish the verified native Android app under `native-android/` on a new branch of `never-seek/novalpie` and attach a downloadable APK to a GitHub Release.

**Architecture:** Keep the existing remote `main` history intact. Clone `main` into a project-local staging directory, add a curated copy of the current `D:\NovalPie\native-android` source while excluding generated outputs, local SDK paths, emulator artifacts, and credentials, then commit and push only that staging tree. Create the release tag on the new branch so its source snapshot and APK refer to the same code line.

**Tech Stack:** Git/GitHub CLI, Android Gradle Plugin, Kotlin/Compose, GitHub Releases.

---

### Task 1: Resolve publication scope and names

**Files:**
- Read: `D:\NovalPie\native-android\README.md`
- Read: `D:\NovalPie\native-android\.gitignore`
- Read: `D:\NovalPie\native-android\app\build.gradle.kts`

- [ ] Verify the remote default branch is `main`, the target branch is `codex/native-android-2.0`, and the release tag is `v2.0.0-native-beta2`.
- [ ] Verify the source APK is the current debug build and record its SHA-256.

### Task 2: Create an isolated publication checkout

**Files:**
- Create: `D:\NovalPie\publish-staging\`

- [ ] Clone `https://github.com/never-seek/novalpie.git` at `main` into `D:\NovalPie\publish-staging`.
- [ ] Create `codex/native-android-2.0` from the remote `main` without changing the current local app checkout.

### Task 3: Add curated native source and repository documentation

**Files:**
- Create: `D:\NovalPie\publish-staging\native-android\`
- Modify: `D:\NovalPie\publish-staging\README.md`
- Create: `D:\NovalPie\publish-staging\native-android\RELEASE_NOTES.md`

- [ ] Copy source, Gradle configuration, tests, docs, and tools from the current app checkout.
- [ ] Exclude `build/`, `.gradle/`, `.kotlin/`, `local.properties`, emulator screenshots/artifacts, Playwright state, crash dumps, APK outputs, signing files, and other machine-local files.
- [ ] Document the native module location, offline build command, APK asset name, SHA-256, known debug-build limitation, and installation instructions.

### Task 4: Verify and package

**Files:**
- Artifact: `D:\NovalPie\native-android\app\build\outputs\apk\debug\app-debug.apk`

- [ ] Run `:app:testReleaseUnitTest --rerun-tasks --offline --no-daemon --max-workers=1 --console=plain`.
- [ ] Run `:app:lintDebug :app:assembleDebug --offline --no-daemon --max-workers=1 --console=plain`.
- [ ] Confirm zero test failures/errors and `No issues found` from lint.
- [ ] Copy the verified APK to the release staging area and compute SHA-256.

### Task 5: Publish branch and release

**Files:**
- Git commit on `codex/native-android-2.0`
- GitHub Release `v2.0.0-native-beta2`

- [ ] Review `git diff --check` and the staged file list for generated or sensitive files.
- [ ] Commit the curated source and documentation with message `publish native Android app 2.0`.
- [ ] Push the branch with upstream tracking.
- [ ] Create the GitHub Release targeting the pushed branch/tag and attach the verified APK with release notes.

### Task 6: Verify public result

- [ ] Confirm the branch commit exists on GitHub.
- [ ] Confirm the Release is published and the APK asset has the expected size and SHA-256.
- [ ] Report branch URL, release URL, APK download URL, validation results, and any debug-signing limitation.
