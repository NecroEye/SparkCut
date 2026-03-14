package com.muratcangzm.templateengine.parser

import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.templateengine.raw.RawTemplateDefinition


interface TemplateSpecParser {
    fun parse(definition: RawTemplateDefinition): TemplateSpec

    fun parseAll(definitions: List<RawTemplateDefinition>): List<TemplateSpec> =
        definitions.map(::parse)
}