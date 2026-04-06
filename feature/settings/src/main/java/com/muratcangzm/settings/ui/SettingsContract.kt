package com.muratcangzm.settings.ui

import androidx.compose.runtime.Immutable
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface SettingsContract {

    @Immutable
    data class State(
        val appVersion: String = "",
        val selectedExportQuality: ExportQuality = ExportQuality.HIGH,
        val selectedDefaultFrameRate: FrameRate = FrameRate.FPS_30,
        val autoSaveEnabled: Boolean = true,
        val autoSaveIntervalSeconds: Int = 30,
        val showWatermark: Boolean = false,
        val hardwareAccelerationEnabled: Boolean = true,
        val maxUndoSteps: Int = 20,
        val cacheUsageLabel: String = "Calculating...",
        val isClearingCache: Boolean = false,
    )

    enum class ExportQuality(val label: String, val shortLabel: String) {
        LOW("Low (480p)", "480p"),
        MEDIUM("Medium (720p)", "720p"),
        HIGH("High (1080p)", "1080p"),
        ULTRA("Ultra (4K)", "4K"),
    }

    enum class FrameRate(val label: String, val fps: Int) {
        FPS_24("24 fps", 24),
        FPS_30("30 fps", 30),
        FPS_60("60 fps", 60),
    }

    sealed interface Event {
        data object BackClicked : Event
        data class ExportQualitySelected(val quality: ExportQuality) : Event
        data class FrameRateSelected(val frameRate: FrameRate) : Event
        data class AutoSaveToggled(val enabled: Boolean) : Event
        data class WatermarkToggled(val enabled: Boolean) : Event
        data class HardwareAccelerationToggled(val enabled: Boolean) : Event
        data object ClearCacheClicked : Event
        data object ResetToDefaultsClicked : Event
    }

    sealed interface Effect {
        data object NavigateBack : Effect
        data class ShowMessage(val message: String) : Effect
    }

    interface Presenter {
        val state: StateFlow<State>
        val effects: SharedFlow<Effect>
        fun onEvent(event: Event)
    }
}
