package dev.etcherfx.camerarefreshfix.xposed

import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** A hook target that either resolved, or didn't and says why. */
sealed interface Target<out T> {
    data class Available<T>(val value: T) : Target<T>
    data class Unavailable(val reason: String) : Target<Nothing>
}

/** Reads a window's windowing mode; returns null if the call fails for any reason. */
class WindowingModeReader(private val method: Method) {
    fun read(window: Any?): Int? = try {
        method.invoke(window) as? Int
    } catch (_: Throwable) {
        null
    }
}

/** Everything option A needs. Both getters must be hooked together, or neither. */
class PinnedWindowTargets(
    val getPreferredMinRefreshRate: Method,
    val getPreferredMaxRefreshRate: Method,
    val windowingMode: WindowingModeReader,
    val pinnedMode: Int,
)

class FrameworkTargets(
    val pinnedWindows: Target<PinnedWindowTargets>,
    /** `RefreshRatePolicy.addRefreshRateRangeForPackage(String, float, float)`, for option B. */
    val cameraRange: Target<Method>,
) {
    /** Class and member names; tests inject fakes. */
    data class Names(
        val refreshRatePolicy: String = "com.android.server.wm.RefreshRatePolicy",
        val windowState: String = "com.android.server.wm.WindowState",
        val windowConfiguration: String = "android.app.WindowConfiguration",
        val getPreferredMinRefreshRate: String = "getPreferredMinRefreshRate",
        val getPreferredMaxRefreshRate: String = "getPreferredMaxRefreshRate",
        val addRefreshRateRangeForPackage: String = "addRefreshRateRangeForPackage",
        val getWindowingMode: String = "getWindowingMode",
        val windowingModePinned: String = "WINDOWING_MODE_PINNED",
    )

    companion object {
        /** Resolves every target with plain reflection. Never throws. */
        fun resolve(classLoader: ClassLoader, names: Names = Names()): FrameworkTargets {
            val policy = try {
                Class.forName(names.refreshRatePolicy, false, classLoader)
            } catch (t: Throwable) {
                val reason = "${names.refreshRatePolicy} not found: $t"
                return FrameworkTargets(Target.Unavailable(reason), Target.Unavailable(reason))
            }
            return FrameworkTargets(
                pinnedWindows = resolvePinned(classLoader, names, policy),
                cameraRange = resolveCameraRange(names, policy),
            )
        }

        private fun resolvePinned(
            classLoader: ClassLoader,
            names: Names,
            policy: Class<*>,
        ): Target<PinnedWindowTargets> = try {
            val windowState = Class.forName(names.windowState, false, classLoader)
            val getMin = declaredMethod(policy, names.getPreferredMinRefreshRate, Float::class.javaPrimitiveType!!, windowState)
            val getMax = declaredMethod(policy, names.getPreferredMaxRefreshRate, Float::class.javaPrimitiveType!!, windowState)
            val getMode = hierarchyMethod(windowState, names.getWindowingMode, Int::class.javaPrimitiveType!!)
            val pinnedMode = staticIntField(Class.forName(names.windowConfiguration, false, classLoader), names.windowingModePinned)
            Target.Available(PinnedWindowTargets(getMin, getMax, WindowingModeReader(getMode), pinnedMode))
        } catch (t: Throwable) {
            Target.Unavailable(t.message ?: t.toString())
        }

        private fun resolveCameraRange(names: Names, policy: Class<*>): Target<Method> = try {
            Target.Available(
                declaredMethod(
                    policy,
                    names.addRefreshRateRangeForPackage,
                    Void.TYPE,
                    String::class.java,
                    Float::class.javaPrimitiveType!!,
                    Float::class.javaPrimitiveType!!,
                ),
            )
        } catch (t: Throwable) {
            Target.Unavailable(t.message ?: t.toString())
        }

        private fun declaredMethod(owner: Class<*>, name: String, returnType: Class<*>, vararg params: Class<*>): Method {
            val method = try {
                owner.getDeclaredMethod(name, *params)
            } catch (_: NoSuchMethodException) {
                throw NoSuchMethodException("${owner.name}.$name(${params.joinToString { it.name }}) not found")
            }
            check(method.returnType == returnType) {
                "${owner.name}.$name returns ${method.returnType.name}, expected ${returnType.name}"
            }
            check(!Modifier.isStatic(method.modifiers)) { "${owner.name}.$name is static" }
            method.isAccessible = true
            return method
        }

        private fun hierarchyMethod(start: Class<*>, name: String, returnType: Class<*>): Method {
            var type: Class<*>? = start
            while (type != null) {
                val method = type.declaredMethods.firstOrNull { it.name == name && it.parameterCount == 0 }
                if (method != null) {
                    check(method.returnType == returnType) {
                        "${type.name}.$name returns ${method.returnType.name}, expected ${returnType.name}"
                    }
                    method.isAccessible = true
                    return method
                }
                type = type.superclass
            }
            throw NoSuchMethodException("$name() not found on ${start.name} or its superclasses")
        }

        private fun staticIntField(owner: Class<*>, name: String): Int {
            val field = try {
                owner.getDeclaredField(name)
            } catch (_: NoSuchFieldException) {
                throw NoSuchFieldException("${owner.name}.$name not found")
            }
            check(Modifier.isStatic(field.modifiers) && field.type == Int::class.javaPrimitiveType) {
                "${owner.name}.$name is not a static int"
            }
            field.isAccessible = true
            return field.getInt(null)
        }
    }
}
