package com.muratcangzm.media.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.muratcangzm.media.data.internal.MediaAssetIdFactory
import com.muratcangzm.media.data.internal.MimeTypeResolver
import com.muratcangzm.media.data.internal.readCommonMetadata
import com.muratcangzm.media.data.internal.readImageMetadata
import com.muratcangzm.media.data.internal.readVideoMetadata
import com.muratcangzm.media.domain.MediaAssetResolver
import com.muratcangzm.media.domain.model.MediaResolveBatchResult
import com.muratcangzm.media.domain.model.MediaResolveFailure
import com.muratcangzm.model.media.MediaAsset
import com.muratcangzm.model.media.MediaSourceType
import com.muratcangzm.model.media.MediaType

class AndroidMediaAssetResolver(
    private val context: Context,
) : MediaAssetResolver {

    override fun resolve(uri: String): MediaAsset? {
        return runCatching { resolveOrThrow(uri) }.getOrNull()
    }

    override fun resolveAll(uris: List<String>): MediaResolveBatchResult {
        val resolved = mutableListOf<MediaAsset>()
        val failures = mutableListOf<MediaResolveFailure>()

        uris
            .asSequence()
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
            .forEach { uriString ->
                runCatching {
                    resolveOrThrow(uriString)
                }.onSuccess { asset ->
                    resolved += asset
                }.onFailure { throwable ->
                    failures += MediaResolveFailure(
                        uri = uriString,
                        reason = throwable.message ?: "Unknown media parsing error.",
                    )
                }
            }

        return MediaResolveBatchResult(
            assets = resolved,
            failures = failures,
        )
    }

    private fun resolveOrThrow(uriString: String): MediaAsset {
        val uri = Uri.parse(uriString)
        require(!uriString.isBlank()) { "Media uri cannot be blank." }
        require(
            uri.scheme == ContentResolver.SCHEME_CONTENT ||
                    uri.scheme == ContentResolver.SCHEME_FILE
        ) {
            "Unsupported media uri scheme: ${uri.scheme}"
        }

        val commonMetadata = readCommonMetadata(
            contentResolver = context.contentResolver,
            uri = uri,
        )

        val mimeType = MimeTypeResolver.resolveMimeType(
            resolverMimeType = context.contentResolver.getType(uri),
            fileName = commonMetadata.fileName,
            uriString = uriString,
        )

        val mediaType = MimeTypeResolver.resolveMediaType(mimeType)
            ?: error("Unsupported media type for uri: $uriString")

        return when (mediaType) {
            MediaType.IMAGE -> {
                val imageMetadata = readImageMetadata(
                    context = context,
                    uri = uri,
                )

                MediaAsset(
                    id = MediaAssetIdFactory.fromUri(uriString),
                    uri = uriString,
                    type = MediaType.IMAGE,
                    mimeType = mimeType,
                    fileName = commonMetadata.fileName,
                    sizeBytes = commonMetadata.sizeBytes,
                    width = imageMetadata.width,
                    height = imageMetadata.height,
                    durationMs = null,
                    rotationDegrees = imageMetadata.rotationDegrees,
                    sourceType = MediaSourceType.LOCAL_GALLERY,
                    addedAtEpochMillis = null,
                )
            }

            MediaType.VIDEO -> {
                val videoMetadata = readVideoMetadata(
                    context = context,
                    uri = uri,
                )

                MediaAsset(
                    id = MediaAssetIdFactory.fromUri(uriString),
                    uri = uriString,
                    type = MediaType.VIDEO,
                    mimeType = mimeType,
                    fileName = commonMetadata.fileName,
                    sizeBytes = commonMetadata.sizeBytes,
                    width = videoMetadata.width,
                    height = videoMetadata.height,
                    durationMs = videoMetadata.durationMs,
                    rotationDegrees = videoMetadata.rotationDegrees,
                    sourceType = MediaSourceType.LOCAL_GALLERY,
                    addedAtEpochMillis = null,
                )
            }
        }
    }
}