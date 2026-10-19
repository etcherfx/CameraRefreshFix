<h1 align="center">
  <img src=".github/assets/banner.png" alt="CameraRefreshFix" width="850">
</h1>

<div align="center">

<img alt="kotlin" height="56" src="https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/built-with/kotlin_vector.svg">

[![Release](https://img.shields.io/github/v/release/etcherfx/CameraRefreshFix?style=for-the-badge)](https://github.com/etcherfx/CameraRefreshFix/releases/latest)
[![Build](https://img.shields.io/github/actions/workflow/status/etcherfx/CameraRefreshFix/ci.yml?style=for-the-badge)](https://github.com/etcherfx/CameraRefreshFix/actions/workflows/ci.yml)

**An Xposed module that keeps Android at full refresh rate when a floating camera window would cap it.**

[Getting started](#getting-started) · [Settings](#settings) · [The problem](#the-problem) · [Compatibility](#compatibility)

</div>

## Features

- **Ignore picture-in-picture windows (on by default):** Pinned windows no longer contribute a preferred minimum or maximum refresh rate, matching how Android already treats `preferredModeId`. Full-screen camera apps keep stock behavior.
- **Remove the camera cap (off by default):** Drops the camera's refresh-rate limit entirely. Android adds it to avoid dropped camera frames, so enable it only if you accept that tradeoff.

It is a modern-API Xposed module (libxposed API 101), scoped to the System Framework (`system_server`).

## Getting started

You need a rooted device with an Xposed framework that implements libxposed API 101 or newer (see [Compatibility](#compatibility)).

1. Download `CameraRefreshFix-X.Y.Z.apk` from the [latest release](https://github.com/etcherfx/CameraRefreshFix/releases/latest) and install it.
2. In your Xposed manager, enable CameraRefreshFix and make sure **System Framework** is in its scope.
3. Reboot. The framework only loads modules into `system_server` at boot.

The module works with its defaults right away; the settings screen is optional.

## Settings

Open the settings from the module's entry in your Xposed manager; the module has no launcher icon. The status line at the top says whether the framework is connected and the System Framework is in scope. The switches stay disabled until the module is active.

| Setting | Default | What it does | Takes effect |
|---|---|---|---|
| Ignore picture-in-picture windows | On | A floating (pinned) window no longer sets the refresh-rate range for the whole display | Immediately |
| Remove the camera cap | Off | No app is capped at 60 Hz while it uses the camera, full-screen camera apps included | The next time an app turns its camera on; a camera already running keeps its cap until it stops |

## The problem

When an app uses the camera, Android's `CameraServiceProxy` registers a refresh-rate range of at most 60 Hz for that package through `WindowManagerInternal.addRefreshRateRangeForPackage`. `DisplayContent` then walks the windows from top to bottom, and the first visible window with a nonzero preferred minimum or maximum refresh rate sets the range for the whole display.

Android skips picture-in-picture (pinned) windows for `preferredModeId`, but not for the minimum and maximum refresh rate. A floating video-call window with the camera on therefore caps every app underneath it to 60 Hz. The Force peak refresh rate setting can't override this, because the app-request vote outranks the user's minimum refresh rate.

The module hooks `com.android.server.wm.RefreshRatePolicy` in `system_server`:

- With **Ignore picture-in-picture windows** on, `getPreferredMinRefreshRate` and `getPreferredMaxRefreshRate` return 0 for pinned windows, so the window below decides the range.
- With **Remove the camera cap** on, `addRefreshRateRangeForPackage` doesn't store the camera's range at all.

If a hook target is missing or has a different signature on your Android build, the module skips that feature and logs why in the framework's module log. It never stops `system_server`.

## Compatibility

| Item | Status |
|---|---|
| Android | 12 or newer; tested on Android 17 (Pixel 10 Pro) |
| Xposed frameworks | Any that implements libxposed API 101 or newer. Tested with Vector 2.2 (the JingMatrix LSPosed fork); Vector 2.1 and newer qualify |
| Frameworks with API 100 only (LSPosed 1.9.x, Vector 2.0) | Not supported: they can't load modern API 101 modules |
| Samsung, Xiaomi and other OEM Android builds | Untested |

## Development

Everything Android-related builds inside a Docker container; see [CONTRIBUTING.md](CONTRIBUTING.md) for the commands and [AGENTS.md](AGENTS.md) for the code map.

## License

[GPL-3.0](LICENSE)
