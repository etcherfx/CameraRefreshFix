package dev.etcherfx.camerarefreshfix.core

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsTest {
    @Test
    fun missingKeysUseTheDefaults() {
        val settings = Settings.fromLookup { _, default -> default }
        assertEquals(Settings(ignorePinnedWindows = true, removeCameraCap = false), settings)
        assertEquals(Settings.DEFAULT, settings)
    }

    @Test
    fun storedValuesOverrideTheDefaults() {
        val stored = mapOf(Settings.KEY_IGNORE_PINNED_WINDOWS to false, Settings.KEY_REMOVE_CAMERA_CAP to true)
        val settings = Settings.fromLookup { key, default -> stored[key] ?: default }
        assertEquals(Settings(ignorePinnedWindows = false, removeCameraCap = true), settings)
    }

    @Test
    fun eachKeyIsReadIndependently() {
        val settings = Settings.fromLookup { key, default -> if (key == Settings.KEY_REMOVE_CAMERA_CAP) true else default }
        assertEquals(Settings(ignorePinnedWindows = true, removeCameraCap = true), settings)
    }
}
