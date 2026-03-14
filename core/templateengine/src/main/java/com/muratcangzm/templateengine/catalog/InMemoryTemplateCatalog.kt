package com.muratcangzm.templateengine.catalog

import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.template.TemplateCategory
import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.templateengine.parser.DefaultTemplateSpecParser
import com.muratcangzm.templateengine.parser.TemplateSpecParser
import com.muratcangzm.templateengine.raw.RawTemplateDefinition
import com.muratcangzm.templateengine.validation.DefaultTemplateValidator
import com.muratcangzm.templateengine.validation.TemplateValidationResult
import com.muratcangzm.templateengine.validation.TemplateValidator

class InMemoryTemplateCatalog(
    rawDefinitions: List<RawTemplateDefinition>,
    parser: TemplateSpecParser = DefaultTemplateSpecParser(),
    validator: TemplateValidator = DefaultTemplateValidator(),
) : TemplateCatalog {

    private val templates: List<TemplateSpec> =
        parser.parseAll(rawDefinitions).sortedBy { it.sortOrder }

    private val validationResults: List<TemplateValidationResult> =
        validator.validateAll(templates)

    init {
        val invalidTemplates = validationResults.filterNot { it.isValid }
        require(invalidTemplates.isEmpty()) {
            buildString {
                appendLine("Invalid template catalog:")
                invalidTemplates.forEach { result ->
                    appendLine(" - ${result.templateId.value}")
                    result.errors.forEach { error ->
                        appendLine("   * ${error.code}: ${error.message}")
                    }
                }
            }
        }
    }

    override fun getAll(): List<TemplateSpec> = templates

    override fun getFeatured(): List<TemplateSpec> =
        templates.filter { it.isFeatured }

    override fun getByCategory(category: TemplateCategory): List<TemplateSpec> =
        templates.filter { it.category == category }

    override fun getById(id: TemplateId): TemplateSpec? =
        templates.firstOrNull { it.id == id }

    override fun search(query: String): List<TemplateSpec> {
        val normalized = query.trim().lowercase()
        if (normalized.isBlank()) return templates

        return templates.filter { template ->
            template.name.lowercase().contains(normalized) ||
                    template.description.lowercase().contains(normalized) ||
                    template.tags.any { it.lowercase().contains(normalized) } ||
                    template.category.name.lowercase().contains(normalized)
        }
    }

    override fun getValidationResults(): List<TemplateValidationResult> = validationResults
}