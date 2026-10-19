package dev.etcherfx.camerarefreshfix.core

/** The module's two options, stored in the framework's remote preferences group [GROUP]. */
data class Settings(
    /** Option A: pinned (picture-in-picture) windows contribute no preferred refresh-rate range. */
    val ignorePinnedWindows: Boolean,
    /** Option B: the camera refresh-rate range is never stored, so the camera cap is gone everywhere. */
    val removeCameraCap: Boolean,
) {
    companion object {
        const val GROUP = "settings"
        const val KEY_IGNORE_PINNED_WINDOWS = "ignore_pinned_windows"
        const val KEY_REMOVE_CAMERA_CAP = "remove_camera_cap"
        const val DEFAULT_IGNORE_PINNED_WINDOWS = true
        const val DEFAULT_REMOVE_CAMERA_CAP = false

        val DEFAULT = Settings(DEFAULT_IGNORE_PINNED_WINDOWS, DEFAULT_REMOVE_CAMERA_CAP)

        /** Reads both options through [lookup], which returns the default for a missing key. */
        fun fromLookup(lookup: (key: String, default: Boolean) -> Boolean) = Settings(
            ignorePinnedWindows = lookup(KEY_IGNORE_PINNED_WINDOWS, DEFAULT_IGNORE_PINNED_WINDOWS),
            removeCameraCap = lookup(KEY_REMOVE_CAMERA_CAP, DEFAULT_REMOVE_CAMERA_CAP),
        )
    }
}
