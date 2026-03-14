package com.muratcangzm.home.di

import com.muratcangzm.home.ui.HomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val homeModule = module {
    viewModel { HomeViewModel(templateCatalog = get()) }
}