package com.muratcangzm.template.di

import com.muratcangzm.template.ui.TemplateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val featureTemplateModule = module {
    viewModel{ TemplateViewModel(templateCatalog = get()) }
}