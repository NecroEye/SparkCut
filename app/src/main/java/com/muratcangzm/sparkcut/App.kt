package com.muratcangzm.sparkcut

import android.app.Application
import com.muratcangzm.create.di.createModule
import com.muratcangzm.data.di.dataModule
import com.muratcangzm.database.di.databaseModule
import com.muratcangzm.editor.di.editorModule
import com.muratcangzm.export.di.exportModule
import com.muratcangzm.home.di.homeModule
import com.muratcangzm.media.di.mediaCoreModule
import com.muratcangzm.template.di.featureTemplateModule
import com.muratcangzm.templateengine.di.templateCoreModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {

    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(
                featureTemplateModule,
                homeModule,
                featureTemplateModule,
                createModule,
                editorModule,
                exportModule,
                mediaCoreModule,
                templateCoreModule,
                dataModule,
                databaseModule,
            )
        }
    }
}