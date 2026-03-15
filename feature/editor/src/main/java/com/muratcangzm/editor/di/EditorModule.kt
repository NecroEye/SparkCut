package com.muratcangzm.editor.di

import com.muratcangzm.editor.ui.EditorViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val editorModule = module {
    viewModel { (templateId: String?, mediaUris: List<String>, projectId: String?) ->
        EditorViewModel(
            templateCatalog = get(),
            mediaAssetResolver = get(),
            templateIdArg = templateId,
            mediaUrisArg = mediaUris,
            projectSessionManager = get(),
            projectIdArg = projectId,
            renderValidationEngine = get(),
        )
    }
}
