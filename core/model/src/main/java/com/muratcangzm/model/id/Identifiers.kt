package com.muratcangzm.model.id

@JvmInline
value class TemplateId(val value: String) {
    init {
        require(value.isNotBlank()) { "TemplateId cannot be blank." }
    }

    override fun toString(): String = value
}

@JvmInline
value class ProjectId(val value: String) {
    init {
        require(value.isNotBlank()) { "ProjectId cannot be blank." }
    }

    override fun toString(): String = value
}

@JvmInline
value class SlotId(val value: String) {
    init {
        require(value.isNotBlank()) { "SlotId cannot be blank." }
    }

    override fun toString(): String = value
}

@JvmInline
value class TextFieldId(val value: String) {
    init {
        require(value.isNotBlank()) { "TextFieldId cannot be blank." }
    }

    override fun toString(): String = value
}

@JvmInline
value class MediaAssetId(val value: String) {
    init {
        require(value.isNotBlank()) { "MediaAssetId cannot be blank." }
    }

    override fun toString(): String = value
}

@JvmInline
value class AudioTrackId(val value: String) {
    init {
        require(value.isNotBlank()) { "AudioTrackId cannot be blank." }
    }

    override fun toString(): String = value
}

@JvmInline
value class ExportJobId(val value: String) {
    init {
        require(value.isNotBlank()) { "ExportJobId cannot be blank." }
    }

    override fun toString(): String = value
}