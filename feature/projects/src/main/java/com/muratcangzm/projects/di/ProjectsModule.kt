package com.muratcangzm.projects.di

import com.muratcangzm.projects.ui.ProjectsViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val projectModule = module {
    viewModel { ProjectsViewModel(projectSessionManager = get()) }
}