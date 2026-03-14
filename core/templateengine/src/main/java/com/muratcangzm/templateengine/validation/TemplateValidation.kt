package com.muratcangzm.templateengine.validation

import com.muratcangzm.model.id.TemplateId
import com.muratcangzm.model.template.TemplateSpec

enum class ValidationSeverity {
    ERROR,
    WARNING,
}

data class TemplateValidationIssue(
    val code: String,
    val severity: ValidationSeverity,
    val message: String,
)

data class TemplateValidationResult(
    val templateId: TemplateId,
    val issues: List<TemplateValidationIssue>,
) {
    val isValid: Boolean
        get() = issues.none { it.severity == ValidationSeverity.ERROR }

    val errors: List<TemplateValidationIssue>
        get() = issues.filter { it.severity == ValidationSeverity.ERROR }

    val warnings: List<TemplateValidationIssue>
        get() = issues.filter { it.severity == ValidationSeverity.WARNING }
}

interface TemplateValidator {
    fun validate(template: TemplateSpec): TemplateValidationResult

    fun validateAll(templates: List<TemplateSpec>): List<TemplateValidationResult> =
        templates.map(::validate)
}