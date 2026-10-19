package dev.etcherfx.camerarefreshfix.core

object RefreshDecision {
    /**
     * Whether a window's preferred min/max refresh rate should be reported as 0. This mirrors
     * DisplayContent's own rule, which skips pinned windows for preferredModeId only.
     * An unknown windowing mode never intervenes.
     */
    fun shouldZeroForWindow(settings: Settings, windowingMode: Int?, pinnedMode: Int): Boolean =
        settings.ignorePinnedWindows && windowingMode != null && windowingMode == pinnedMode

    /** Whether a package's camera refresh-rate range should be dropped instead of stored. */
    fun shouldSkipCameraRange(settings: Settings): Boolean = settings.removeCameraCap
}
