package com.muratcangzm.export.di

import com.muratcangzm.export.ui.ExportContract
import com.muratcangzm.export.ui.ExportViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val exportModule = module {
    viewModel { (launchArgs: ExportContract.LaunchArgs) ->
        ExportViewModel(
            templateCatalog = get(),
            mediaExportEngine = get(),
            mediaExportFilePublisher = get(),
            mediaAssetResolver = get(),
            templateRenderPlanner = get(),
            launchArgs = launchArgs,
            audioTrackMetadataReader = get(),
            projectSessionManager = get(),
            renderValidationEngine = get(),
        )
    }
}
