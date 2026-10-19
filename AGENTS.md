# CameraRefreshFix

A modern-API Xposed module (libxposed API 101, compiled against 102) scoped to the System Framework (`system_server`, scope name `system`). It keeps the display at full refresh rate when a floating (picture-in-picture) camera window would cap it at 60 Hz. Tested target: Vector, the JingMatrix LSPosed fork.

## Setup and verification

Builds and tests run inside a Docker container that carries the Android SDK and JDK 21. Don't install anything Android-related on the host, and don't assume the SDK exists outside the container.

```sh
docker build -t camerarefreshfix-build docker
docker run --rm -v "$PWD:/work" -v crf-gradle:/root/.gradle camerarefreshfix-build ./gradlew check assembleDebug assembleRelease
```

- `check` runs the JVM unit tests and lint (warnings are errors).
- Outputs: `app/build/outputs/apk/debug/app-debug.apk` and `app/build/outputs/apk/release/app-release-unsigned.apk` (signed only when the `RELEASE_KEYSTORE_*` variables are set; see CONTRIBUTING).
- Hooks can only be verified on a device: install the debug APK, enable the module with the System Framework scope, reboot, and read the framework's module log (tag `CameraRefreshFix`).
- If a JS tool is ever needed, use Bun; there is no Node or npm.

## Code map

- `app/src/main/kotlin/dev/etcherfx/camerarefreshfix/core/`: pure logic, no Android or Xposed types: `Settings` (pref keys and defaults), `RefreshDecision`, `OnceLogger`.
- `.../xposed/`: runs in `system_server`. `ModuleEntry` (entry point), `FrameworkTargets` (reflective resolution, plain reflection so it's unit-testable), `HookInstaller` (the three hooks), `PrefsBridge` (remote preferences into a volatile `Settings`).
- `.../ModuleApp.kt`, `.../SettingsActivity.kt`: the settings screen, a Compose Material 3 Expressive screen (Material3 1.5 beta; move to stable when released), opened from the Xposed manager (`MODULE_SETTINGS` category, no launcher icon).
- `app/src/main/resources/META-INF/xposed/`: `java_init.list`, `scope.list`, `module.prop` (modern-API module metadata).
- `app/src/test/`: JVM unit tests; `xposed/fakes/` holds stand-ins for the framework classes.
- `docker/Dockerfile`: the build environment.
- `.github/workflows/ci.yml`: builds and checks on push and pull request.
- `.github/workflows/release.yml`: tag-driven release; checks the tag against `version` in `gradle.properties`, signs the APK, attaches it to a draft, then publishes the notes from `.github/release-notes/vX.Y.Z.md`.

## Constraints and conventions

- American English in code, comments, docs and user-facing text.
- Conventional Commits with a DCO sign-off (`git commit -s`).
- Never commit machine paths, personal paths, hostnames, device serials or network names. Keystores and passwords never enter the repo.
- `system_server` must never crash because of this module: resolve every target reflectively, catch every Throwable around decisions, and keep `chain.proceed()` outside those try blocks so the original method's exceptions propagate as in stock.
- Target API 101: don't use members added in API 102 (`HookHandle.getId`/`replaceHook`, `HookBuilder.setId`, `onHotReloading`/`onHotReloaded`, `detach`, service `getRunningTargets`/`hotReloadModule`).
- No R8 in release builds: the release APK has the same bytecode as the device-tested debug build, and `java_init.list` stays valid.
- The package name `dev.etcherfx.camerarefreshfix` and the signing key are permanent; the framework stores the enabled flag and scope under the package.
- Behavior A (default on): pinned windows contribute no preferred min/max refresh rate. Full-screen camera apps keep stock behavior. Applies live.
- Behavior B (default off, opt-in): remove the camera refresh cap entirely. Android adds the cap to avoid dropped camera frames, so it stays off by default. Applies from the next camera start.
- Pref group `settings`, keys `ignore_pinned_windows` (default true) and `remove_camera_cap` (default false).

## Gotchas

- `DisplayContent` lets the first visible window with a nonzero preferred min/max refresh rate decide the range for the whole display. Android skips pinned windows for `preferredModeId` only, which is the bug.
- Option A needs both getters hooked: one alone leaves an asymmetric range that still caps the display, so `HookInstaller` unhooks the other.
- Force peak refresh rate can't override the camera cap: the app-request vote outranks the user minimum-refresh setting.
- Samsung, Xiaomi and other OEM frameworks are untested.
- Releases are immutable on this repo: the release workflow creates a draft, attaches the APK, then publishes.
