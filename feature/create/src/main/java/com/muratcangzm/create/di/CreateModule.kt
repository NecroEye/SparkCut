package com.muratcangzm.create.di

import com.muratcangzm.create.ui.CreateViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val createModule = module {
    viewModel { (templateId: String) ->
        CreateViewModel(
            templateCatalog = get(),
            mediaAssetResolver = get(),
            templateIdArg = templateId,
        )
    }
}