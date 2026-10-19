@file:Suppress("unused", "UNUSED_PARAMETER")

package dev.etcherfx.camerarefreshfix.xposed.fakes

// Stand-ins for the framework classes, shaped like com.android.server.wm and android.app.

open class FakeConfigurationContainer(private val mode: Int) {
    fun getWindowingMode(): Int = mode
}

class FakeWindowState(mode: Int) : FakeConfigurationContainer(mode)

class FakeThrowingWindowState {
    fun getWindowingMode(): Int = throw IllegalStateException("boom")
}

class FakeWindowConfiguration {
    companion object {
        @JvmField
        val WINDOWING_MODE_PINNED = 2
    }
}

class FakeWindowConfigurationWithoutPinned

class FakeRefreshRatePolicy {
    fun getPreferredMinRefreshRate(w: FakeWindowState): Float = 0f
    fun getPreferredMaxRefreshRate(w: FakeWindowState): Float = 0f
    fun addRefreshRateRangeForPackage(packageName: String, minRefreshRate: Float, maxRefreshRate: Float) {}
}

class FakePolicyWithoutMaxGetter {
    fun getPreferredMinRefreshRate(w: FakeWindowState): Float = 0f
    fun addRefreshRateRangeForPackage(packageName: String, minRefreshRate: Float, maxRefreshRate: Float) {}
}

class FakePolicyWithIntGetter {
    fun getPreferredMinRefreshRate(w: FakeWindowState): Int = 0
    fun getPreferredMaxRefreshRate(w: FakeWindowState): Float = 0f
    fun addRefreshRateRangeForPackage(packageName: String, minRefreshRate: Float, maxRefreshRate: Float) {}
}

class FakePolicyWithoutAdd {
    fun getPreferredMinRefreshRate(w: FakeWindowState): Float = 0f
    fun getPreferredMaxRefreshRate(w: FakeWindowState): Float = 0f
}
