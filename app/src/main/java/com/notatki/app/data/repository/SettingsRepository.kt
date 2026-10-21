package com.notatki.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.notatki.app.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

class SettingsRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeStr = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val handStr = prefs.getString("handedness", Handedness.RIGHT_HANDED.name) ?: Handedness.RIGHT_HANDED.name
        val angleStr = prefs.getString("angle_unit", AngleUnit.DEG.name) ?: AngleUnit.DEG.name
        val stylusOnly = prefs.getBoolean("stylus_only", false)
        val paperStr = prefs.getString("default_paper", PaperStyle.GRID.name) ?: PaperStyle.GRID.name
        val defaultUrl = "http://matieusz.fun/notatki/version.json"
        val serverUrl = prefs.getString("update_server_url", defaultUrl) ?: defaultUrl
        val immersive = prefs.getBoolean("immersive_mode", true)

        return AppSettings(
            themeMode = try { AppThemeMode.valueOf(themeStr) } catch (e: Exception) { AppThemeMode.SYSTEM },
            handedness = try { Handedness.valueOf(handStr) } catch (e: Exception) { Handedness.RIGHT_HANDED },
            defaultAngleUnit = try { AngleUnit.valueOf(angleStr) } catch (e: Exception) { AngleUnit.DEG },
            defaultStylusOnly = stylusOnly,
            defaultPaperStyle = try { PaperStyle.valueOf(paperStr) } catch (e: Exception) { PaperStyle.GRID },
            updateServerUrl = serverUrl,
            immersiveMode = immersive
        )
    }

    fun updateThemeMode(mode: AppThemeMode) {
        prefs.edit().putString("theme_mode", mode.name).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun updateHandedness(handedness: Handedness) {
        prefs.edit().putString("handedness", handedness.name).apply()
        _settings.value = _settings.value.copy(handedness = handedness)
    }

    fun updateDefaultAngleUnit(angleUnit: AngleUnit) {
        prefs.edit().putString("angle_unit", angleUnit.name).apply()
        _settings.value = _settings.value.copy(defaultAngleUnit = angleUnit)
    }

    fun updateDefaultStylusOnly(stylusOnly: Boolean) {
        prefs.edit().putBoolean("stylus_only", stylusOnly).apply()
        _settings.value = _settings.value.copy(defaultStylusOnly = stylusOnly)
    }

    fun updateDefaultPaperStyle(style: PaperStyle) {
        prefs.edit().putString("default_paper", style.name).apply()
        _settings.value = _settings.value.copy(defaultPaperStyle = style)
    }

    fun updateServerUrl(url: String) {
        prefs.edit().putString("update_server_url", url).apply()
        _settings.value = _settings.value.copy(updateServerUrl = url)
    }

    fun updateImmersiveMode(immersive: Boolean) {
        prefs.edit().putBoolean("immersive_mode", immersive).apply()
        _settings.value = _settings.value.copy(immersiveMode = immersive)
    }

    fun calculateStorageUsage(): String {
        return try {
            val notesDir = File(context.filesDir, "notes")
            val exportsDir = File(context.filesDir, "exports")
            var totalBytes = 0L

            fun dirSize(dir: File): Long {
                var size = 0L
                dir.listFiles()?.forEach { file ->
                    size += if (file.isDirectory) dirSize(file) else file.length()
                }
                return size
            }

            if (notesDir.exists()) totalBytes += dirSize(notesDir)
            if (exportsDir.exists()) totalBytes += dirSize(exportsDir)

            val kb = totalBytes / 1024.0
            val mb = kb / 1024.0

            if (mb >= 1.0) {
                String.format(java.util.Locale.US, "%.1f MB", mb)
            } else {
                String.format(java.util.Locale.US, "%.1f KB", kb)
            }
        } catch (e: Exception) {
            "0 KB"
        }
    }
}
