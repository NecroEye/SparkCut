package com.muratcangzm.editor.di

import com.muratcangzm.editor.ui.EditorViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val editorModule = module {
    viewModel { (mediaUris: List<String>, projectId: String?) ->
        EditorViewModel(
            mediaAssetResolver = get(),
            mediaUrisArg = mediaUris,
            projectSessionManager = get(),
            projectIdArg = projectId,
            applicationContext = androidContext(),
        )
    }
}
