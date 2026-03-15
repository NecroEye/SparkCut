package com.muratcangzm.templateengine.di

import com.muratcangzm.templateengine.catalog.InMemoryTemplateCatalog
import com.muratcangzm.templateengine.catalog.TemplateCatalog
import com.muratcangzm.templateengine.parser.DefaultTemplateSpecParser
import com.muratcangzm.templateengine.parser.TemplateSpecParser
import com.muratcangzm.templateengine.planner.DefaultTemplateRenderPlanner
import com.muratcangzm.templateengine.planner.TemplateRenderPlanner
import com.muratcangzm.templateengine.render.DefaultRenderValidationEngine
import com.muratcangzm.templateengine.render.RenderValidationEngine
import com.muratcangzm.templateengine.seed.SparkCutTemplateSeed
import com.muratcangzm.templateengine.validation.DefaultTemplateValidator
import com.muratcangzm.templateengine.validation.TemplateValidator
import org.koin.dsl.module

val templateCoreModule = module {
    single<TemplateSpecParser> { DefaultTemplateSpecParser() }

    single<TemplateValidator> { DefaultTemplateValidator() }

    single<TemplateRenderPlanner> { DefaultTemplateRenderPlanner() }

    single<RenderValidationEngine> { DefaultRenderValidationEngine() }

    single<TemplateCatalog> {
        InMemoryTemplateCatalog(
            rawDefinitions = SparkCutTemplateSeed.all(),
            parser = get(),
            validator = get(),
        )
    }
}