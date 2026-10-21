package com.notatki.app.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notatki.app.NotatkiApp
import com.notatki.app.data.model.*
import com.notatki.app.util.UpdateCheckResult
import com.notatki.app.util.UpdateInfo
import com.notatki.app.util.UpdateManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NotatkiApp
    private val repo = app.settingsRepository

    val settings: StateFlow<AppSettings> = repo.settings

    private val _isCheckingUpdate = MutableStateFlow(false)
    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()

    private val _updateCheckResult = MutableStateFlow<UpdateCheckResult?>(null)
    val updateCheckResult: StateFlow<UpdateCheckResult?> = _updateCheckResult.asStateFlow()

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress.asStateFlow()

    fun getStorageUsage(): String = repo.calculateStorageUsage()

    fun getCurrentVersionName(): String = UpdateManager.getCurrentVersionName(app)
    fun getCurrentVersionCode(): Int = UpdateManager.getCurrentVersionCode(app)

    fun setThemeMode(mode: AppThemeMode) = repo.updateThemeMode(mode)
    fun setHandedness(handedness: Handedness) = repo.updateHandedness(handedness)
    fun setAngleUnit(unit: AngleUnit) = repo.updateDefaultAngleUnit(unit)
    fun setStylusOnly(stylusOnly: Boolean) = repo.updateDefaultStylusOnly(stylusOnly)
    fun setDefaultPaperStyle(style: PaperStyle) = repo.updateDefaultPaperStyle(style)
    fun setUpdateServerUrl(url: String) = repo.updateServerUrl(url)
    fun setImmersiveMode(immersive: Boolean) = repo.updateImmersiveMode(immersive)

    fun checkForUpdates() {
        viewModelScope.launch {
            _isCheckingUpdate.value = true
            _updateCheckResult.value = null
            val result = UpdateManager.checkForUpdates(app, settings.value.updateServerUrl)
            _updateCheckResult.value = result
            _isCheckingUpdate.value = false
        }
    }

    fun clearUpdateCheckResult() {
        _updateCheckResult.value = null
        _downloadProgress.value = null
    }

    fun downloadAndInstallUpdate(info: UpdateInfo, onComplete: () -> Unit) {
        viewModelScope.launch {
            _downloadProgress.value = 0f
            UpdateManager.downloadAndInstallApk(
                context = app,
                apkUrl = info.apkUrl,
                onProgress = { progress -> _downloadProgress.value = progress },
                onResult = { result ->
                    _downloadProgress.value = null
                    onComplete()
                }
            )
        }
    }
}
