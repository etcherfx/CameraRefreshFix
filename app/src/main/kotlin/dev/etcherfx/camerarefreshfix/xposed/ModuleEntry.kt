package dev.etcherfx.camerarefreshfix.xposed

import android.util.Log
import dev.etcherfx.camerarefreshfix.BuildConfig
import dev.etcherfx.camerarefreshfix.core.OnceLogger
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.ModuleLoadedParam
import io.github.libxposed.api.XposedModuleInterface.SystemServerStartingParam
import java.util.concurrent.atomic.AtomicBoolean

/** Entry point listed in META-INF/xposed/java_init.list. Hooks system_server only. */
class ModuleEntry : XposedModule() {
    private var systemServer = false
    private val installed = AtomicBoolean(false)

    // Kept for the life of system_server: the hooks read the bridge's settings.
    private var prefs: PrefsBridge? = null
    private var hooks: HookInstaller? = null

    override fun onModuleLoaded(param: ModuleLoadedParam) {
        try {
            systemServer = param.isSystemServer
        } catch (_: Throwable) {
            systemServer = false
        }
    }

    override fun onSystemServerStarting(param: SystemServerStartingParam) {
        if (!systemServer || !installed.compareAndSet(false, true)) return
        try {
            val logLine: (Int, String) -> Unit = { level, message -> log(level, TAG, message) }
            val once = OnceLogger { level, message, error ->
                if (error == null) log(level, TAG, message) else log(level, TAG, message, error)
            }
            log(
                Log.INFO,
                TAG,
                "CameraRefreshFix ${BuildConfig.VERSION_NAME} on $frameworkName $frameworkVersion " +
                    "($frameworkVersionCode, API $apiVersion, properties 0x${frameworkProperties.toString(16)})",
            )
            val bridge = PrefsBridge(this, once, logLine).also { it.start() }
            prefs = bridge
            log(Log.INFO, TAG, "Settings: ${PrefsBridge.describe(bridge.settings)}")
            val installer = HookInstaller(this, { bridge.settings }, once, logLine)
            installer.install(FrameworkTargets.resolve(param.classLoader))
            hooks = installer
        } catch (t: Throwable) {
            try {
                log(Log.ERROR, TAG, "Setup failed; module inactive", t)
            } catch (_: Throwable) {
                // Nothing else is safe to do in system_server.
            }
        }
    }

    private companion object {
        const val TAG = "CameraRefreshFix"
    }
}
