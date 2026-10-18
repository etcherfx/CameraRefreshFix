# CameraRefreshFix

An Xposed module for LSPosed-compatible frameworks (tested target: Vector, the JingMatrix LSPosed fork), scoped to the System Framework (`android`, `system_server`). It keeps the display at full refresh rate when a floating (picture-in-picture) camera window would cap it at 60 Hz. The repository currently holds only the scaffold; the Kotlin and Gradle implementation is still to be added.

## Setup and verification

- Kotlin + Gradle Android app module.
- Builds and tests run inside a Docker container that carries the Android SDK and a JDK. Don't install anything Android-related on the host, and don't assume the SDK exists outside the container.
- Use the Gradle wrapper inside the container once the project has one.
- If a JS tool is ever needed, use Bun; there is no Node or npm.

## Code map

- `.github/workflows/release.yml`: tag-driven release; publishes the notes from `.github/release-notes/vX.Y.Z.md`.
- `.github/release-notes/`: one hand-written notes file per release.

## Constraints and conventions

- American English in code, comments, docs and user-facing text.
- Conventional Commits with a DCO sign-off (`git commit -s`).
- Never commit machine paths, personal paths, hostnames, device serials or network names.
- Behavior A (default on): pinned windows contribute no preferred min/max refresh rate. Full-screen camera apps keep stock behavior.
- Behavior B (default off, opt-in): remove the camera refresh cap entirely. Android adds the cap to avoid dropped camera frames, so it stays off by default.

## Gotchas

- `DisplayContent` lets the first visible window with a nonzero preferred min/max refresh rate decide the range for the whole display. Android skips pinned windows for `preferredModeId` only, which is the bug.
- Force peak refresh rate can't override the camera cap: the app-request vote outranks the user minimum-refresh setting.
- Samsung, Xiaomi and other OEM frameworks are untested.
- Releases are immutable on this repo: the release workflow creates a draft, attaches files, then publishes. Any APK step must attach before publishing.
