package com.muratcangzm.templateengine.catalog

import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.template.TemplateCategory
import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.templateengine.validation.TemplateValidationResult

interface TemplateCatalog {
    fun getAll(): List<TemplateSpec>
    fun getFeatured(): List<TemplateSpec>
    fun getByCategory(category: TemplateCategory): List<TemplateSpec>
    fun getById(id: TemplateId): TemplateSpec?
    fun search(query: String): List<TemplateSpec>
    fun getValidationResults(): List<TemplateValidationResult>
}