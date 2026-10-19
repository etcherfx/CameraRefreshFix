package dev.etcherfx.camerarefreshfix.core

import org.junit.Assert.assertEquals
import org.junit.Test

class RefreshDecisionTest {
    private val pinned = 2
    private val fullscreen = 1
    private val freeform = 5

    @Test
    fun zeroesOnlyPinnedWindowsWhenOptionAIsOn() {
        for (a in listOf(true, false)) {
            for (b in listOf(true, false)) {
                val settings = Settings(ignorePinnedWindows = a, removeCameraCap = b)
                assertEquals("A=$a B=$b pinned", a, RefreshDecision.shouldZeroForWindow(settings, pinned, pinned))
                assertEquals("A=$a B=$b fullscreen", false, RefreshDecision.shouldZeroForWindow(settings, fullscreen, pinned))
                assertEquals("A=$a B=$b freeform", false, RefreshDecision.shouldZeroForWindow(settings, freeform, pinned))
                assertEquals("A=$a B=$b unknown", false, RefreshDecision.shouldZeroForWindow(settings, null, pinned))
            }
        }
    }

    @Test
    fun skipsTheCameraRangeOnlyWhenOptionBIsOn() {
        for (a in listOf(true, false)) {
            assertEquals(true, RefreshDecision.shouldSkipCameraRange(Settings(ignorePinnedWindows = a, removeCameraCap = true)))
            assertEquals(false, RefreshDecision.shouldSkipCameraRange(Settings(ignorePinnedWindows = a, removeCameraCap = false)))
        }
    }
}
