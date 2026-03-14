package com.muratcangzm.media.data.internal

import com.muratcangzm.model.id.MediaAssetId
import java.security.MessageDigest

internal object MediaAssetIdFactory {

    fun fromUri(uri: String): MediaAssetId {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(uri.toByteArray(Charsets.UTF_8))
            .joinToString(separator = "") { byte -> "%02x".format(byte) }

        return MediaAssetId(digest)
    }
}