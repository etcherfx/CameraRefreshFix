<h1 align="center">
  <img src=".github/assets/banner.png" alt="CameraRefreshFix" width="850">
</h1>

<div align="center">

<img alt="kotlin" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/kotlin_vector.svg">

**An Xposed module that keeps Android at full refresh rate when a floating camera window would cap it.**

[Features](#features) · [The problem](#the-problem) · [Compatibility](#compatibility)

</div>

> [!NOTE]
> The module is not implemented yet; this repository only holds the project scaffold.

## Features

- **Pinned windows ignored (on by default):** Picture-in-picture windows no longer contribute a preferred minimum or maximum refresh rate, matching how Android already treats `preferredModeId`. Full-screen camera apps keep stock behavior.
- **Remove the camera cap (off by default):** Drops the camera's refresh-rate limit entirely. Android adds it to avoid dropped camera frames, so enable it only if you accept that tradeoff.

It is an Xposed module for LSPosed-compatible frameworks, scoped to the System Framework (`android`, `system_server`).

## The problem

When an app uses the camera, Android's `CameraServiceProxy` registers a refresh-rate range of at most 60 Hz for that package through `WindowManagerInternal.addRefreshRateRangeForPackage`. `DisplayContent` then walks the windows from top to bottom, and the first visible window with a nonzero preferred minimum or maximum refresh rate sets the range for the whole display.

Android skips picture-in-picture (pinned) windows for `preferredModeId`, but not for the minimum and maximum refresh rate. A floating video-call window with the camera on therefore caps every app underneath it to 60 Hz. The Force peak refresh rate setting can't override this, because the app-request vote outranks the user's minimum refresh rate.

## Compatibility

| Item | Status |
|---|---|
| Reported on | Pixel 10 Pro, Android 17, Discord video call in picture-in-picture |
| Tested framework | Vector (the JingMatrix LSPosed fork) |
| Samsung, Xiaomi and other OEM frameworks | Untested |

## Development

See [CONTRIBUTING.md](CONTRIBUTING.md) and [AGENTS.md](AGENTS.md).

## License

[GPL-3.0](LICENSE)
