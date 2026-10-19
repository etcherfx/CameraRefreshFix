package dev.etcherfx.camerarefreshfix.xposed

import dev.etcherfx.camerarefreshfix.xposed.fakes.FakePolicyWithIntGetter
import dev.etcherfx.camerarefreshfix.xposed.fakes.FakePolicyWithoutAdd
import dev.etcherfx.camerarefreshfix.xposed.fakes.FakePolicyWithoutMaxGetter
import dev.etcherfx.camerarefreshfix.xposed.fakes.FakeRefreshRatePolicy
import dev.etcherfx.camerarefreshfix.xposed.fakes.FakeThrowingWindowState
import dev.etcherfx.camerarefreshfix.xposed.fakes.FakeWindowConfiguration
import dev.etcherfx.camerarefreshfix.xposed.fakes.FakeWindowConfigurationWithoutPinned
import dev.etcherfx.camerarefreshfix.xposed.fakes.FakeWindowState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameworkTargetsTest {
    private val names = FrameworkTargets.Names(
        refreshRatePolicy = FakeRefreshRatePolicy::class.java.name,
        windowState = FakeWindowState::class.java.name,
        windowConfiguration = FakeWindowConfiguration::class.java.name,
    )

    private fun resolve(names: FrameworkTargets.Names) = FrameworkTargets.resolve(javaClass.classLoader!!, names)

    @Test
    fun resolvesEveryTarget() {
        val targets = resolve(names)

        val pinned = (targets.pinnedWindows as Target.Available).value
        assertEquals("getPreferredMinRefreshRate", pinned.getPreferredMinRefreshRate.name)
        assertEquals("getPreferredMaxRefreshRate", pinned.getPreferredMaxRefreshRate.name)
        assertEquals(2, pinned.pinnedMode)
        // getWindowingMode is declared on the superclass, as on ConfigurationContainer.
        assertEquals(2, pinned.windowingMode.read(FakeWindowState(2)))
        assertEquals("addRefreshRateRangeForPackage", (targets.cameraRange as Target.Available).value.name)
    }

    @Test
    fun missingGetterDisablesOnlyOptionA() {
        val targets = resolve(names.copy(refreshRatePolicy = FakePolicyWithoutMaxGetter::class.java.name))
        assertUnavailable(targets.pinnedWindows, "getPreferredMaxRefreshRate")
        assertTrue(targets.cameraRange is Target.Available)
    }

    @Test
    fun wrongReturnTypeDisablesOptionA() {
        val targets = resolve(names.copy(refreshRatePolicy = FakePolicyWithIntGetter::class.java.name))
        assertUnavailable(targets.pinnedWindows, "returns int")
        assertTrue(targets.cameraRange is Target.Available)
    }

    @Test
    fun missingAddDisablesOnlyOptionB() {
        val targets = resolve(names.copy(refreshRatePolicy = FakePolicyWithoutAdd::class.java.name))
        assertTrue(targets.pinnedWindows is Target.Available)
        assertUnavailable(targets.cameraRange, "addRefreshRateRangeForPackage")
    }

    @Test
    fun missingPinnedConstantDisablesOptionA() {
        val targets = resolve(names.copy(windowConfiguration = FakeWindowConfigurationWithoutPinned::class.java.name))
        assertUnavailable(targets.pinnedWindows, "WINDOWING_MODE_PINNED")
        assertTrue(targets.cameraRange is Target.Available)
    }

    @Test
    fun missingPolicyClassDisablesBoth() {
        val targets = resolve(names.copy(refreshRatePolicy = "com.example.Missing"))
        assertUnavailable(targets.pinnedWindows, "com.example.Missing")
        assertUnavailable(targets.cameraRange, "com.example.Missing")
    }

    @Test
    fun windowingModeReaderReturnsNullWhenTheCallFails() {
        val reader = WindowingModeReader(FakeThrowingWindowState::class.java.getMethod("getWindowingMode"))
        assertNull(reader.read(FakeThrowingWindowState()))
        assertNull(reader.read(null))
        assertNull(reader.read("not a window"))
    }

    private fun assertUnavailable(target: Target<*>, reasonContains: String) {
        val reason = (target as Target.Unavailable).reason
        assertTrue("'$reason' should mention '$reasonContains'", reason.contains(reasonContains))
    }
}
