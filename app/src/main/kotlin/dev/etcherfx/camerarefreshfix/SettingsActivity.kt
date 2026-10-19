package dev.etcherfx.camerarefreshfix

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import dev.etcherfx.camerarefreshfix.core.Settings
import io.github.libxposed.service.XposedService

class SettingsActivity : ComponentActivity() {
    private var ui by mutableStateOf(SettingsUi(emptyList(), attention = false, Settings.DEFAULT, editable = false))
    private var prefs: SharedPreferences? = null
    private val app get() = application as ModuleApp
    private val onServiceChanged: () -> Unit = { render() }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            SettingsTheme {
                SettingsScreen(
                    ui = ui,
                    onIgnorePinnedChange = { write(Settings.KEY_IGNORE_PINNED_WINDOWS, it) },
                    onRemoveCapChange = { write(Settings.KEY_REMOVE_CAMERA_CAP, it) },
                )
            }
        }
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
        val service = app.service
        val status = service?.let(::connect)
            ?: Connection(listOf(getString(R.string.status_inactive)), attention = true, prefs = null)
        val p = status.prefs
        prefs = p
        ui = SettingsUi(
            statusLines = status.lines,
            attention = status.attention,
            settings = p?.let { Settings.fromLookup { key, default -> it.getBoolean(key, default) } } ?: Settings.DEFAULT,
            editable = p != null,
        )
    }

    /** Builds the status lines and returns them with the preferences, or null preferences if they can't be used. */
    private fun connect(service: XposedService): Connection = try {
        val lines = mutableListOf(
            getString(R.string.status_connected, service.frameworkName, service.frameworkVersion, service.apiVersion),
        )
        val missingScope = SYSTEM_SCOPE !in service.scope
        if (missingScope) lines += getString(R.string.status_missing_scope)
        val prefs = if (service.frameworkProperties and XposedService.PROP_CAP_REMOTE != 0L) {
            service.getRemotePreferences(Settings.GROUP)
        } else {
            lines += getString(R.string.status_no_remote_prefs)
            null
        }
        Connection(lines, attention = missingScope || prefs == null, prefs)
    } catch (e: RuntimeException) {
        Connection(listOf(getString(R.string.status_error, e.message ?: e.toString())), attention = true, prefs = null)
    }

    private fun write(key: String, value: Boolean) {
        val p = prefs ?: return
        try {
            p.edit { putBoolean(key, value) }
            ui = ui.copy(
                settings = when (key) {
                    Settings.KEY_IGNORE_PINNED_WINDOWS -> ui.settings.copy(ignorePinnedWindows = value)
                    else -> ui.settings.copy(removeCameraCap = value)
                },
            )
        } catch (e: RuntimeException) {
            render()
            ui = ui.copy(
                statusLines = ui.statusLines + getString(R.string.status_error, e.message ?: e.toString()),
                attention = true,
            )
        }
    }

    private class Connection(val lines: List<String>, val attention: Boolean, val prefs: SharedPreferences?)

    private companion object {
        /** The scope name the framework uses for system_server (the System Framework). */
        const val SYSTEM_SCOPE = "system"
    }
}

private data class SettingsUi(
    val statusLines: List<String>,
    /** Something needs the user's attention: the card uses the error colors. */
    val attention: Boolean,
    val settings: Settings,
    /** The preferences can be written; otherwise the options are disabled. */
    val editable: Boolean,
)

@Composable
private fun SettingsTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    MaterialExpressiveTheme(
        // minSdk 31 always has dynamic color, so there is no fallback palette.
        colorScheme = if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context),
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}

@Composable
private fun SettingsScreen(
    ui: SettingsUi,
    onIgnorePinnedChange: (Boolean) -> Unit,
    onRemoveCapChange: (Boolean) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            StatusCard(ui.statusLines, ui.attention)
            Spacer(Modifier.height(24.dp))
            Column(verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)) {
                OptionItem(
                    index = 0,
                    title = stringResource(R.string.ignore_pinned_title),
                    summary = stringResource(R.string.ignore_pinned_summary),
                    checked = ui.settings.ignorePinnedWindows,
                    enabled = ui.editable,
                    onCheckedChange = onIgnorePinnedChange,
                )
                OptionItem(
                    index = 1,
                    title = stringResource(R.string.remove_cap_title),
                    summary = stringResource(R.string.remove_cap_summary),
                    checked = ui.settings.removeCameraCap,
                    enabled = ui.editable,
                    onCheckedChange = onRemoveCapChange,
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusCard(lines: List<String>, attention: Boolean) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (attention) colors.errorContainer else colors.secondaryContainer,
            contentColor = if (attention) colors.onErrorContainer else colors.onSecondaryContainer,
        ),
    ) {
        Text(
            text = lines.joinToString("\n"),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun OptionItem(
    index: Int,
    title: String,
    summary: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    // The click overload adds no semantics of its own, so the row is announced as one switch.
    SegmentedListItem(
        onClick = { onCheckedChange(!checked) },
        shapes = ListItemDefaults.segmentedShapes(index = index, count = OPTION_COUNT),
        modifier = Modifier.semantics {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
        },
        enabled = enabled,
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null, enabled = enabled) },
    ) {
        Text(title)
    }
}

private const val OPTION_COUNT = 2
