package com.muratcangzm.media.data.internal

import android.webkit.MimeTypeMap
import com.muratcangzm.model.media.MediaType

internal object MimeTypeResolver {

    fun resolveMimeType(
        resolverMimeType: String?,
        fileName: String?,
        uriString: String,
    ): String? {
        if (!resolverMimeType.isNullOrBlank()) return resolverMimeType

        val extensionFromName = fileName
            ?.substringAfterLast('.', missingDelimiterValue = "")
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }

        val extensionFromUri = MimeTypeMap.getFileExtensionFromUrl(uriString)
            ?.lowercase()
            ?.takeIf { it.isNotBlank() }

        val extension = extensionFromName ?: extensionFromUri
        if (extension.isNullOrBlank()) return null

        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    fun resolveMediaType(mimeType: String?): MediaType? = when {
        mimeType.isNullOrBlank() -> null
        mimeType.startsWith("image/") -> MediaType.IMAGE
        mimeType.startsWith("video/") -> MediaType.VIDEO
        else -> null
    }
}