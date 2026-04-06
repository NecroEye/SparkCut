package com.muratcangzm.settings.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(
    private val applicationContext: Context,
) : ViewModel(), SettingsContract.Presenter {

    private val _state = MutableStateFlow(SettingsContract.State())
    override val state: StateFlow<SettingsContract.State> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<SettingsContract.Effect>(
        replay = 0,
        extraBufferCapacity = 16,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val effects: SharedFlow<SettingsContract.Effect> = _effects.asSharedFlow()

    init {
        loadInitialState()
    }

    override fun onEvent(event: SettingsContract.Event) {
        when (event) {
            SettingsContract.Event.BackClicked -> {
                _effects.tryEmit(SettingsContract.Effect.NavigateBack)
            }

            is SettingsContract.Event.ExportQualitySelected -> {
                _state.update { it.copy(selectedExportQuality = event.quality) }
            }

            is SettingsContract.Event.FrameRateSelected -> {
                _state.update { it.copy(selectedDefaultFrameRate = event.frameRate) }
            }

            is SettingsContract.Event.AutoSaveToggled -> {
                _state.update { it.copy(autoSaveEnabled = event.enabled) }
            }

            is SettingsContract.Event.WatermarkToggled -> {
                _state.update { it.copy(showWatermark = event.enabled) }
            }

            is SettingsContract.Event.HardwareAccelerationToggled -> {
                _state.update { it.copy(hardwareAccelerationEnabled = event.enabled) }
            }

            SettingsContract.Event.ClearCacheClicked -> clearCache()

            SettingsContract.Event.ResetToDefaultsClicked -> resetToDefaults()
        }
    }

    private fun loadInitialState() {
        viewModelScope.launch {
            val version = runCatching {
                val packageInfo = applicationContext.packageManager
                    .getPackageInfo(applicationContext.packageName, 0)
                "${packageInfo.versionName} (${packageInfo.longVersionCode})"
            }.getOrDefault("1.0.0")

            val cacheSize = withContext(Dispatchers.IO) {
                calculateCacheSize()
            }

            _state.update {
                it.copy(
                    appVersion = version,
                    cacheUsageLabel = formatCacheSize(cacheSize),
                )
            }
        }
    }

    private fun clearCache() {
        if (_state.value.isClearingCache) return

        _state.update { it.copy(isClearingCache = true) }

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                applicationContext.cacheDir.deleteRecursively()
                applicationContext.cacheDir.mkdirs()
                applicationContext.externalCacheDir?.deleteRecursively()
                applicationContext.externalCacheDir?.mkdirs()
            }

            delay(400L)

            val newSize = withContext(Dispatchers.IO) { calculateCacheSize() }

            _state.update {
                it.copy(
                    isClearingCache = false,
                    cacheUsageLabel = formatCacheSize(newSize),
                )
            }

            _effects.tryEmit(SettingsContract.Effect.ShowMessage("Cache cleared"))
        }
    }

    private fun resetToDefaults() {
        _state.update {
            it.copy(
                selectedExportQuality = SettingsContract.ExportQuality.HIGH,
                selectedDefaultFrameRate = SettingsContract.FrameRate.FPS_30,
                autoSaveEnabled = true,
                showWatermark = false,
                hardwareAccelerationEnabled = true,
                maxUndoSteps = 20,
            )
        }
        _effects.tryEmit(SettingsContract.Effect.ShowMessage("Settings reset to defaults"))
    }

    private fun calculateCacheSize(): Long {
        var totalSize = 0L
        totalSize += directorySize(applicationContext.cacheDir)
        applicationContext.externalCacheDir?.let {
            totalSize += directorySize(it)
        }
        return totalSize
    }

    private fun directorySize(dir: File): Long {
        if (!dir.exists()) return 0L
        return dir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun formatCacheSize(bytes: Long): String {
        return when {
            bytes < 1024L -> "$bytes B"
            bytes < 1024L * 1024L -> "${"%.1f".format(bytes / 1024.0)} KB"
            bytes < 1024L * 1024L * 1024L -> "${"%.1f".format(bytes / (1024.0 * 1024.0))} MB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
        }
    }
}
