package dev.etcherfx.camerarefreshfix.xposed

import android.util.Log
import dev.etcherfx.camerarefreshfix.core.OnceLogger
import dev.etcherfx.camerarefreshfix.core.RefreshDecision
import dev.etcherfx.camerarefreshfix.core.Settings
import io.github.libxposed.api.XposedInterface
import io.github.libxposed.api.XposedInterface.ExceptionMode
import io.github.libxposed.api.XposedInterface.HookHandle
import io.github.libxposed.api.XposedInterface.Hooker
import java.lang.reflect.Method

/**
 * Builds the three hooks. Every decision runs inside try/catch and falls back to stock behavior;
 * chain.proceed() stays outside it, so exceptions from the original method propagate as in stock.
 */
class HookInstaller(
    private val xposed: XposedInterface,
    private val settings: () -> Settings,
    private val once: OnceLogger,
    private val log: (level: Int, message: String) -> Unit,
) {
    /** Kept so the hooks stay reachable for the life of system_server. */
    val handles = mutableListOf<HookHandle>()

    fun install(targets: FrameworkTargets) {
        when (val pinned = targets.pinnedWindows) {
            is Target.Available -> installPinnedWindows(pinned.value)
            is Target.Unavailable -> log(Log.WARN, "Option A unavailable, not hooked: ${pinned.reason}")
        }
        when (val range = targets.cameraRange) {
            is Target.Available -> installCameraRange(range.value)
            is Target.Unavailable -> log(Log.WARN, "Option B unavailable, not hooked: ${range.reason}")
        }
    }

    private fun installPinnedWindows(targets: PinnedWindowTargets) {
        val hooker = pinnedWindowHooker(targets)
        val min = hook(targets.getPreferredMinRefreshRate, hooker) ?: return
        val max = hook(targets.getPreferredMaxRefreshRate, hooker)
        if (max == null) {
            // One getter alone still leaves an asymmetric range that caps the display.
            try {
                min.unhook()
            } catch (t: Throwable) {
                once.log("unhook-min", Log.WARN, t) { "Could not unhook ${describe(targets.getPreferredMinRefreshRate)}" }
            }
            log(Log.WARN, "Option A disabled: both refresh-rate getters must be hooked")
            return
        }
        handles += min
        handles += max
    }

    private fun installCameraRange(method: Method) {
        hook(method, cameraRangeHooker())?.let { handles += it }
    }

    private fun hook(method: Method, hooker: Hooker): HookHandle? = try {
        xposed.hook(method).setExceptionMode(ExceptionMode.PROTECTIVE).intercept(hooker).also {
            log(Log.INFO, "Hooked ${describe(method)}")
        }
    } catch (t: Throwable) {
        once.log("hook-${method.name}", Log.WARN, t) { "Could not hook ${describe(method)}" }
        null
    }

    private fun pinnedWindowHooker(targets: PinnedWindowTargets) = Hooker { chain ->
        val result = chain.proceed()
        var zero = false
        try {
            // Only nonzero results can cap the display, so only those need the windowing mode.
            if (result is Float && result != 0f) {
                val window = chain.args[0]
                zero = RefreshDecision.shouldZeroForWindow(settings(), targets.windowingMode.read(window), targets.pinnedMode)
                if (zero) {
                    once.log("zeroed-pinned", Log.INFO) { "Ignoring the refresh-rate range of pinned window(s) of ${owningPackage(window)}" }
                }
            }
        } catch (t: Throwable) {
            zero = false
            once.log("pinned-decision", Log.WARN, t) { "Option A decision failed; keeping stock behavior" }
        }
        if (zero) 0f else result
    }

    private fun cameraRangeHooker() = Hooker { chain ->
        var skip = false
        try {
            skip = RefreshDecision.shouldSkipCameraRange(settings())
            if (skip) {
                once.log("skipped-range", Log.INFO) { "Dropping the camera refresh-rate range for ${chain.args[0]}" }
            }
        } catch (t: Throwable) {
            skip = false
            once.log("range-decision", Log.WARN, t) { "Option B decision failed; keeping stock behavior" }
        }
        if (skip) null else chain.proceed()
    }

    private fun owningPackage(window: Any?): String = try {
        window?.javaClass?.getMethod("getOwningPackage")?.invoke(window)?.toString() ?: "unknown"
    } catch (_: Throwable) {
        "unknown"
    }

    private fun describe(method: Method) =
        "${method.declaringClass.simpleName}.${method.name}(${method.parameterTypes.joinToString { it.simpleName }})"
}
