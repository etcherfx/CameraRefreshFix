package dev.etcherfx.camerarefreshfix

import android.app.Activity
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Switch
import android.widget.TextView
import dev.etcherfx.camerarefreshfix.core.Settings
import io.github.libxposed.service.XposedService

class SettingsActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var ignorePinned: Switch
    private lateinit var removeCap: Switch
    private val app get() = application as ModuleApp
    private val onServiceChanged: () -> Unit = { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        status = findViewById(R.id.status)
        ignorePinned = findViewById(R.id.ignore_pinned)
        removeCap = findViewById(R.id.remove_cap)
    }

    override fun onStart() {
        super.onStart()
        app.addServiceListener(onServiceChanged)
        render()
    }

    override fun onStop() {
        app.removeServiceListener(onServiceChanged)
        super.onStop()
    }

    private fun render() {
        // Detach listeners while setting the switches, so rendering doesn't write back.
        ignorePinned.setOnCheckedChangeListener(null)
        removeCap.setOnCheckedChangeListener(null)

        val service = app.service
        if (service == null) status.setText(R.string.status_inactive)
        val prefs = service?.let(::connect)
        ignorePinned.isEnabled = prefs != null
        removeCap.isEnabled = prefs != null
        val current = prefs?.let { p -> Settings.fromLookup { key, default -> p.getBoolean(key, default) } } ?: Settings.DEFAULT
        ignorePinned.isChecked = current.ignorePinnedWindows
        removeCap.isChecked = current.removeCameraCap

        if (prefs == null) return
        ignorePinned.setOnCheckedChangeListener { _, checked -> write(prefs, Settings.KEY_IGNORE_PINNED_WINDOWS, checked) }
        removeCap.setOnCheckedChangeListener { _, checked -> write(prefs, Settings.KEY_REMOVE_CAMERA_CAP, checked) }
    }

    /** Fills in the status line and returns the preferences, or null if they can't be used. */
    private fun connect(service: XposedService): SharedPreferences? = try {
        val lines = mutableListOf(
            getString(R.string.status_connected, service.frameworkName, service.frameworkVersion, service.apiVersion),
        )
        if (SYSTEM_SCOPE !in service.scope) lines += getString(R.string.status_missing_scope)
        val prefs = if (service.frameworkProperties and XposedService.PROP_CAP_REMOTE != 0L) {
            service.getRemotePreferences(Settings.GROUP)
        } else {
            lines += getString(R.string.status_no_remote_prefs)
            null
        }
        status.text = lines.joinToString("\n")
        prefs
    } catch (e: RuntimeException) {
        status.text = getString(R.string.status_error, e.message ?: e.toString())
        null
    }

    private fun write(prefs: SharedPreferences, key: String, value: Boolean) {
        try {
            prefs.edit().putBoolean(key, value).apply()
        } catch (e: RuntimeException) {
            render()
            status.append("\n" + getString(R.string.status_error, e.message ?: e.toString()))
        }
    }

    private companion object {
        /** The scope name the framework uses for system_server (the System Framework). */
        const val SYSTEM_SCOPE = "system"
    }
}
