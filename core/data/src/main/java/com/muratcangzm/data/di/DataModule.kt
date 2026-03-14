package com.muratcangzm.data.di

import com.muratcangzm.data.project.ProjectDraftRepository
import com.muratcangzm.data.project.RoomProjectDraftRepository
import com.muratcangzm.data.projectsession.DefaultProjectSessionManager
import com.muratcangzm.data.projectsession.ProjectSessionManager
import com.muratcangzm.data.session.DataStoreProjectSessionStore
import com.muratcangzm.data.session.ProjectSessionStore
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val dataModule = module {
    single<ProjectDraftRepository> {
        RoomProjectDraftRepository(
            projectDao = get(),
        )
    }

    single<ProjectSessionStore> {
        DataStoreProjectSessionStore(
            context = androidContext(),
        )
    }

    single<ProjectSessionManager> {
        DefaultProjectSessionManager(
            repository = get(),
            sessionStore = get(),
        )
    }
}
