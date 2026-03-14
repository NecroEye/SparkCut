package com.muratcangzm.media.data.export

import android.graphics.Color
import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.StyleSpan
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.StaticOverlaySettings
import androidx.media3.effect.TextOverlay
import androidx.media3.effect.TextureOverlay
import com.google.common.collect.ImmutableList
import com.muratcangzm.media.domain.export.ExportTextGravity
import com.muratcangzm.media.domain.export.ExportTextOverlay

internal object Media3OverlayEffectFactory {

    @OptIn(UnstableApi::class)
    fun createTextOverlayEffect(
        overlays: List<ExportTextOverlay>,
    ): OverlayEffect? {
        val textureOverlays = overlays
            .filter { it.text.isNotBlank() }
            .map { overlay ->
                createTextOverlay(overlay)
            }

        return if (textureOverlays.isEmpty()) {
            null
        } else {
            OverlayEffect(
                ImmutableList.copyOf<TextureOverlay>(textureOverlays)
            )
        }
    }

    @OptIn(UnstableApi::class)
    private fun createTextOverlay(
        overlay: ExportTextOverlay,
    ): TextureOverlay {
        val (anchorX, anchorY) = overlay.gravity.toOverlayAnchor()

        val settings = StaticOverlaySettings.Builder()
            .setOverlayFrameAnchor(anchorX, anchorY)
            .build()

        val spannable = SpannableString(overlay.text).apply {
            setSpan(
                ForegroundColorSpan(Color.WHITE),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                StyleSpan(Typeface.BOLD),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
            setSpan(
                RelativeSizeSpan((overlay.textSizeSp / 16f).coerceAtLeast(0.8f)),
                0,
                length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE,
            )
        }

        return TextOverlay.createStaticTextOverlay(spannable, settings)
    }

    private fun ExportTextGravity.toOverlayAnchor(): Pair<Float, Float> = when (this) {
        ExportTextGravity.TOP_CENTER -> 0f to 0.78f
        ExportTextGravity.CENTER -> 0f to 0f
        ExportTextGravity.BOTTOM_CENTER -> 0f to -0.78f
    }
}