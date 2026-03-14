package com.muratcangzm.templateengine.planner

import com.muratcangzm.model.media.MediaAsset
import com.muratcangzm.model.media.MediaType
import com.muratcangzm.model.template.TemplateSlotSpec
import com.muratcangzm.model.template.TemplateSpec
import com.muratcangzm.model.template.TemplateTimingStrategy
import com.muratcangzm.model.template.TextFieldType
import kotlin.math.max
import kotlin.math.min

class DefaultTemplateRenderPlanner : TemplateRenderPlanner {

    override fun createPlan(
        template: TemplateSpec,
        assets: List<MediaAsset>,
        textValues: Map<String, String>,
    ): TemplateRenderPlan {
        val usableAssets = assets.take(template.maxMediaCount)
        val usableSlots = template.slots
            .sortedBy { it.index }
            .take(usableAssets.size)

        if (usableAssets.isEmpty() || usableSlots.isEmpty()) {
            return TemplateRenderPlan(
                totalDurationMs = 0L,
                sequenceItems = emptyList(),
                textOverlays = emptyList(),
                transitions = emptyList(),
            )
        }

        val baseTargetDurationMs = resolveBaseTargetDurationMs(
            timingStrategy = template.timingStrategy,
            slotCount = usableSlots.size,
        )

        val sequenceItems = usableSlots.zip(usableAssets).map { (slot, asset) ->
            planSlot(
                slot = slot,
                asset = asset,
                baseTargetDurationMs = baseTargetDurationMs,
            )
        }

        val totalDurationMs = sequenceItems.sumOf { it.durationMs }

        val overlays = planTextOverlays(
            template = template,
            textValues = textValues,
            totalDurationMs = totalDurationMs,
        )

        val transitions = planTransitions(
            template = template,
            sequenceItems = sequenceItems,
        )

        return TemplateRenderPlan(
            totalDurationMs = totalDurationMs,
            sequenceItems = sequenceItems,
            textOverlays = overlays,
            transitions = transitions,
        )
    }

    private fun resolveBaseTargetDurationMs(
        timingStrategy: TemplateTimingStrategy,
        slotCount: Int,
    ): Long {
        if (slotCount <= 0) return 0L

        return when (timingStrategy) {
            is TemplateTimingStrategy.FixedPerSlot -> timingStrategy.durationMs

            is TemplateTimingStrategy.DistributedByCount -> {
                val raw = timingStrategy.totalDurationMs / slotCount
                raw.coerceIn(
                    timingStrategy.minPerSlotMs,
                    timingStrategy.maxPerSlotMs,
                )
            }

            is TemplateTimingStrategy.AudioDriven -> {
                ((timingStrategy.minPerSlotMs + timingStrategy.maxPerSlotMs) / 2L)
                    .coerceIn(timingStrategy.minPerSlotMs, timingStrategy.maxPerSlotMs)
            }
        }
    }

    private fun planSlot(
        slot: TemplateSlotSpec,
        asset: MediaAsset,
        baseTargetDurationMs: Long,
    ): PlannedSequenceItem {
        val preferred = slot.preferredDurationMs ?: baseTargetDurationMs
        val minDuration = slot.minDurationMs ?: 600L
        val maxDuration = slot.maxDurationMs ?: preferred

        val targetDurationMs = preferred.coerceIn(minDuration, maxDuration)

        return when (asset.type) {
            MediaType.IMAGE -> {
                PlannedSequenceItem(
                    slotId = slot.id,
                    uri = asset.uri,
                    mediaType = MediaType.IMAGE,
                    durationMs = targetDurationMs,
                    trimStartMs = null,
                    trimEndMs = null,
                )
            }

            MediaType.VIDEO -> {
                val sourceDurationMs = (asset.durationMs ?: targetDurationMs).coerceAtLeast(600L)
                val finalDurationMs = min(sourceDurationMs, targetDurationMs)
                val trimEndMs = if (sourceDurationMs > finalDurationMs) finalDurationMs else null

                PlannedSequenceItem(
                    slotId = slot.id,
                    uri = asset.uri,
                    mediaType = MediaType.VIDEO,
                    durationMs = finalDurationMs,
                    trimStartMs = 0L,
                    trimEndMs = trimEndMs,
                )
            }
        }
    }

    private fun planTextOverlays(
        template: TemplateSpec,
        textValues: Map<String, String>,
        totalDurationMs: Long,
    ): List<PlannedTextOverlay> {
        if (totalDurationMs <= 0L) return emptyList()

        val normalizedValues = textValues
            .mapValues { it.value.trim() }
            .filterValues { it.isNotBlank() }

        if (normalizedValues.isEmpty()) return emptyList()

        val fieldById = template.textFields.associateBy { it.id.value }

        val titleEndMs = min(2_000L, max(1_200L, (totalDurationMs * 0.22f).toLong()))
        val ctaStartMs = if (totalDurationMs >= 3_000L) {
            max(totalDurationMs - 2_000L, (totalDurationMs * 0.72f).toLong())
        } else {
            max(0L, totalDurationMs - 1_200L)
        }

        val middleStartMs = max(titleEndMs + 150L, (totalDurationMs * 0.35f).toLong())
        val middleEndMs = max(
            middleStartMs + 1_200L,
            min(ctaStartMs - 150L, (totalDurationMs * 0.68f).toLong())
        ).coerceAtMost(max(ctaStartMs - 150L, middleStartMs + 1_200L))

        return normalizedValues.mapNotNull { (fieldId, value) ->
            val field = fieldById[fieldId] ?: return@mapNotNull null

            when (field.type) {
                TextFieldType.TITLE -> PlannedTextOverlay(
                    id = fieldId,
                    text = value,
                    gravity = PlannedOverlayGravity.TOP_CENTER,
                    startTimeMs = 0L,
                    endTimeMs = titleEndMs.coerceAtMost(totalDurationMs),
                    textSizeSp = 24f,
                )

                TextFieldType.SUBTITLE,
                TextFieldType.CAPTION,
                TextFieldType.LABEL -> PlannedTextOverlay(
                    id = fieldId,
                    text = value,
                    gravity = PlannedOverlayGravity.CENTER,
                    startTimeMs = middleStartMs.coerceAtMost(totalDurationMs),
                    endTimeMs = middleEndMs.coerceAtMost(totalDurationMs),
                    textSizeSp = 20f,
                )

                TextFieldType.CTA,
                TextFieldType.PRICE -> PlannedTextOverlay(
                    id = fieldId,
                    text = value,
                    gravity = PlannedOverlayGravity.BOTTOM_CENTER,
                    startTimeMs = ctaStartMs.coerceAtMost(totalDurationMs),
                    endTimeMs = totalDurationMs,
                    textSizeSp = 18f,
                )
            }
        }.filter { it.endTimeMs > it.startTimeMs }
    }

    private fun planTransitions(
        template: TemplateSpec,
        sequenceItems: List<PlannedSequenceItem>,
    ): List<PlannedTransitionWindow> {
        if (sequenceItems.size <= 1) return emptyList()

        return buildList {
            sequenceItems.forEachIndexed { index, item ->
                val safeWindow = when (item.mediaType) {
                    MediaType.IMAGE -> min(280L, item.durationMs / 3L)
                    MediaType.VIDEO -> min(360L, item.durationMs / 4L)
                }.coerceAtLeast(120L)

                if (index > 0) {
                    add(
                        PlannedTransitionWindow(
                            preset = template.defaultTransition,
                            targetIndex = index,
                            phase = TransitionPhase.INTRO,
                            durationMs = safeWindow,
                        )
                    )
                }

                if (index < sequenceItems.lastIndex) {
                    add(
                        PlannedTransitionWindow(
                            preset = template.defaultTransition,
                            targetIndex = index,
                            phase = TransitionPhase.OUTRO,
                            durationMs = safeWindow,
                        )
                    )
                }
            }
        }
    }
}