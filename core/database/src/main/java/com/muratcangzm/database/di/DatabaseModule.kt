package com.muratcangzm.database.di

import androidx.room.Room
import com.muratcangzm.database.SparkCutDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val databaseModule = module {
    single<SparkCutDatabase> {
        Room.databaseBuilder(
            androidContext(),
            SparkCutDatabase::class.java,
            "sparkcut.db",
        ).build()
    }

    single {
        get<SparkCutDatabase>().projectDao()
    }
}
