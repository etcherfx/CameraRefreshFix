package dev.etcherfx.camerarefreshfix.xposed

import android.content.SharedPreferences
import android.util.Log
import dev.etcherfx.camerarefreshfix.core.OnceLogger
import dev.etcherfx.camerarefreshfix.core.Settings
import io.github.libxposed.api.XposedInterface

/**
 * Publishes the module's remote preferences into a volatile [Settings] that the hooks read.
 * If remote preferences are unavailable, the defaults (or the last values read) stay in use.
 */
class PrefsBridge(
    private val xposed: XposedInterface,
    private val once: OnceLogger,
    private val log: (level: Int, message: String) -> Unit,
) {
    @Volatile
    var settings: Settings = Settings.DEFAULT
        private set

    // Strong references: SharedPreferences may hold listeners weakly.
    private var prefs: SharedPreferences? = null
    private var listener: SharedPreferences.OnSharedPreferenceChangeListener? = null

    fun start() {
        try {
            if (xposed.frameworkProperties and XposedInterface.PROP_CAP_REMOTE == 0L) {
                once.log("prefs-unsupported", Log.WARN) {
                    "Remote preferences unsupported by the framework; using defaults: ${describe(settings)}"
                }
                return
            }
            val remote = xposed.getRemotePreferences(Settings.GROUP)
            settings = read(remote)
            val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { changed, _ ->
                try {
                    settings = read(changed)
                    log(Log.INFO, "Settings changed: ${describe(settings)}")
                } catch (t: Throwable) {
                    once.log("prefs-reload", Log.WARN, t) { "Could not reload settings; keeping ${describe(settings)}" }
                }
            }
            remote.registerOnSharedPreferenceChangeListener(changeListener)
            prefs = remote
            listener = changeListener
        } catch (t: Throwable) {
            once.log("prefs-start", Log.WARN, t) {
                "Could not read remote preferences; using ${describe(settings)} until a reboot"
            }
        }
    }

    private fun read(prefs: SharedPreferences) = Settings.fromLookup { key, default -> prefs.getBoolean(key, default) }

    companion object {
        fun describe(settings: Settings) =
            "ignore pinned windows=${settings.ignorePinnedWindows}, remove camera cap=${settings.removeCameraCap}"
    }
}
