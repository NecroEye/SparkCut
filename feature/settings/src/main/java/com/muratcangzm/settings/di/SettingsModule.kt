package com.muratcangzm.settings.di

import com.muratcangzm.settings.ui.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val settingsModule = module {
    viewModel { SettingsViewModel(applicationContext = androidContext()) }
}
