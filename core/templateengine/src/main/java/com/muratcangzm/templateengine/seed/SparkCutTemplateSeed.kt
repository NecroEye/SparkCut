package com.muratcangzm.templateengine.seed

import com.muratcangzm.templateengine.raw.RawTemplateDefinition
import com.muratcangzm.templateengine.raw.RawTemplateMusicPolicyDefinition
import com.muratcangzm.templateengine.raw.RawTemplatePreviewDefinition
import com.muratcangzm.templateengine.raw.RawTemplateSlotDefinition
import com.muratcangzm.templateengine.raw.RawTemplateTextStyleDefinition
import com.muratcangzm.templateengine.raw.RawTextFieldDefinition
import com.muratcangzm.templateengine.raw.RawTimingStrategyDefinition

object SparkCutTemplateSeed {

    fun all(): List<RawTemplateDefinition> = listOf(
        partyBurst(),
        glitchRush(),
        gymTransform(),
        travelPostcard(),
        promoFlash(),
    )

    private fun partyBurst(): RawTemplateDefinition =
        RawTemplateDefinition(
            id = "party_burst",
            name = "Party Burst",
            description = "Fast party reel with punchy transitions and neon energy.",
            category = "PARTY",
            tags = listOf("party", "festival", "friends", "music", "night"),
            minMediaCount = 4,
            maxMediaCount = 8,
            slots = (0 until 8).map { index ->
                RawTemplateSlotDefinition(
                    id = "slot_$index",
                    index = index,
                    acceptedType = "ANY",
                    preferredDurationMs = 1200L,
                    maxDurationMs = 1800L,
                    fillMode = "CENTER_CROP",
                    isHero = index == 0,
                )
            },
            textFields = listOf(
                RawTextFieldDefinition(
                    id = "title",
                    type = "TITLE",
                    label = "Title",
                    placeholder = "Best Night Ever",
                    maxLength = 28,
                    required = true,
                    style = RawTemplateTextStyleDefinition(
                        alignment = "CENTER",
                        animation = "POP",
                        caseMode = "TITLE_CASE",
                    ),
                ),
                RawTextFieldDefinition(
                    id = "caption",
                    type = "CAPTION",
                    label = "Caption",
                    placeholder = "Weekend vibes",
                    maxLength = 36,
                    style = RawTemplateTextStyleDefinition(
                        alignment = "CENTER",
                        animation = "FADE",
                    ),
                ),
            ),
            timingStrategy = RawTimingStrategyDefinition.DistributedByCount(
                totalDurationMs = 9800L,
                minPerSlotMs = 900L,
                maxPerSlotMs = 1700L,
            ),
            defaultTransition = "FLASH",
            musicPolicy = RawTemplateMusicPolicyDefinition(
                selectionPolicy = "RECOMMENDED",
                musicVolume = 1f,
                preserveOriginalClipAudio = false,
                trimBehavior = "AUTO_FIT",
            ),
            preview = RawTemplatePreviewDefinition(
                coverSource = "FIRST_MEDIA",
                previewVideoAssetName = "party_burst_preview.mp4",
            ),
            isFeatured = true,
            sortOrder = 10,
        )

    private fun glitchRush(): RawTemplateDefinition =
        RawTemplateDefinition(
            id = "glitch_rush",
            name = "Glitch Rush",
            description = "RGB split and shake-heavy template for trendy social edits.",
            category = "GLITCH",
            tags = listOf("glitch", "vhs", "rgb", "viral", "edit"),
            minMediaCount = 3,
            maxMediaCount = 7,
            slots = (0 until 7).map { index ->
                RawTemplateSlotDefinition(
                    id = "slot_$index",
                    index = index,
                    acceptedType = if (index == 0) "VIDEO_ONLY" else "ANY",
                    preferredDurationMs = 1000L,
                    maxDurationMs = 1500L,
                    fillMode = "CENTER_CROP",
                    isHero = index == 0,
                )
            },
            textFields = listOf(
                RawTextFieldDefinition(
                    id = "title",
                    type = "TITLE",
                    label = "Hook",
                    placeholder = "Too good to scroll past",
                    maxLength = 30,
                    required = true,
                    style = RawTemplateTextStyleDefinition(
                        animation = "GLITCH",
                        caseMode = "UPPERCASE",
                    ),
                ),
            ),
            timingStrategy = RawTimingStrategyDefinition.AudioDriven(
                minPerSlotMs = 700L,
                maxPerSlotMs = 1300L,
            ),
            defaultTransition = "GLITCH_RGB",
            musicPolicy = RawTemplateMusicPolicyDefinition(
                selectionPolicy = "RECOMMENDED",
                musicVolume = 1f,
                preserveOriginalClipAudio = false,
                trimBehavior = "LOOP",
            ),
            preview = RawTemplatePreviewDefinition(
                coverSource = "FIRST_MEDIA",
                previewVideoAssetName = "glitch_rush_preview.mp4",
            ),
            isFeatured = true,
            sortOrder = 20,
        )

    private fun gymTransform(): RawTemplateDefinition =
        RawTemplateDefinition(
            id = "gym_transform",
            name = "Gym Transform",
            description = "Before and after template for progress and physique updates.",
            category = "FITNESS",
            tags = listOf("gym", "fitness", "before", "after", "progress"),
            minMediaCount = 2,
            maxMediaCount = 4,
            slots = listOf(
                RawTemplateSlotDefinition(
                    id = "before_0",
                    index = 0,
                    acceptedType = "ANY",
                    preferredDurationMs = 1800L,
                    fillMode = "CENTER_CROP",
                    isHero = true,
                ),
                RawTemplateSlotDefinition(
                    id = "after_1",
                    index = 1,
                    acceptedType = "ANY",
                    preferredDurationMs = 1800L,
                    fillMode = "CENTER_CROP",
                ),
                RawTemplateSlotDefinition(
                    id = "detail_2",
                    index = 2,
                    acceptedType = "ANY",
                    preferredDurationMs = 1200L,
                    fillMode = "CENTER_CROP",
                ),
                RawTemplateSlotDefinition(
                    id = "detail_3",
                    index = 3,
                    acceptedType = "ANY",
                    preferredDurationMs = 1200L,
                    fillMode = "CENTER_CROP",
                ),
            ),
            textFields = listOf(
                RawTextFieldDefinition(
                    id = "title",
                    type = "TITLE",
                    label = "Title",
                    placeholder = "90 Day Progress",
                    maxLength = 26,
                    required = true,
                    style = RawTemplateTextStyleDefinition(
                        animation = "POP",
                        caseMode = "UPPERCASE",
                    ),
                ),
                RawTextFieldDefinition(
                    id = "subtitle",
                    type = "SUBTITLE",
                    label = "Subtitle",
                    placeholder = "Discipline wins",
                    maxLength = 28,
                    style = RawTemplateTextStyleDefinition(
                        animation = "SLIDE_UP",
                    ),
                ),
            ),
            timingStrategy = RawTimingStrategyDefinition.FixedPerSlot(
                durationMs = 1600L,
            ),
            defaultTransition = "SLIDE_LEFT",
            musicPolicy = RawTemplateMusicPolicyDefinition(
                selectionPolicy = "OPTIONAL",
                preserveOriginalClipAudio = true,
                clipAudioVolume = 0.35f,
                musicVolume = 0.85f,
            ),
            preview = RawTemplatePreviewDefinition(
                coverSource = "CUSTOM_SLOT",
                coverSlotId = "after_1",
                previewVideoAssetName = "gym_transform_preview.mp4",
            ),
            isFeatured = true,
            sortOrder = 30,
        )

    private fun travelPostcard(): RawTemplateDefinition =
        RawTemplateDefinition(
            id = "travel_postcard",
            name = "Travel Postcard",
            description = "Clean memory template for travel clips and scenic photo sets.",
            category = "TRAVEL",
            tags = listOf("travel", "vacation", "memories", "summer", "trip"),
            minMediaCount = 5,
            maxMediaCount = 10,
            slots = (0 until 10).map { index ->
                RawTemplateSlotDefinition(
                    id = "slot_$index",
                    index = index,
                    acceptedType = "ANY",
                    preferredDurationMs = 1400L,
                    maxDurationMs = 2200L,
                    fillMode = "BLUR_EXTEND",
                    isHero = index == 0,
                )
            },
            textFields = listOf(
                RawTextFieldDefinition(
                    id = "title",
                    type = "TITLE",
                    label = "Destination",
                    placeholder = "Santorini, Greece",
                    maxLength = 30,
                    required = true,
                    style = RawTemplateTextStyleDefinition(
                        animation = "FADE",
                        caseMode = "TITLE_CASE",
                    ),
                ),
                RawTextFieldDefinition(
                    id = "caption",
                    type = "CAPTION",
                    label = "Caption",
                    placeholder = "Sunsets, sea breeze and good food",
                    maxLength = 48,
                    style = RawTemplateTextStyleDefinition(
                        maxLines = 3,
                        animation = "SLIDE_UP",
                    ),
                ),
            ),
            timingStrategy = RawTimingStrategyDefinition.DistributedByCount(
                totalDurationMs = 14500L,
                minPerSlotMs = 1100L,
                maxPerSlotMs = 2200L,
            ),
            defaultTransition = "FADE",
            musicPolicy = RawTemplateMusicPolicyDefinition(
                selectionPolicy = "RECOMMENDED",
                preserveOriginalClipAudio = false,
                musicVolume = 0.9f,
            ),
            preview = RawTemplatePreviewDefinition(
                coverSource = "FIRST_MEDIA",
                previewVideoAssetName = "travel_postcard_preview.mp4",
            ),
            sortOrder = 40,
        )

    private fun promoFlash(): RawTemplateDefinition =
        RawTemplateDefinition(
            id = "promo_flash",
            name = "Promo Flash",
            description = "Small business promo template for products, offers and launches.",
            category = "PROMO",
            tags = listOf("promo", "business", "sale", "product", "offer"),
            minMediaCount = 3,
            maxMediaCount = 6,
            slots = (0 until 6).map { index ->
                RawTemplateSlotDefinition(
                    id = "slot_$index",
                    index = index,
                    acceptedType = "ANY",
                    preferredDurationMs = 1200L,
                    maxDurationMs = 1800L,
                    fillMode = "CENTER_CROP",
                    isHero = index == 0,
                )
            },
            textFields = listOf(
                RawTextFieldDefinition(
                    id = "title",
                    type = "TITLE",
                    label = "Main Title",
                    placeholder = "New Drop",
                    maxLength = 22,
                    required = true,
                    style = RawTemplateTextStyleDefinition(
                        animation = "POP",
                        caseMode = "UPPERCASE",
                    ),
                ),
                RawTextFieldDefinition(
                    id = "price",
                    type = "PRICE",
                    label = "Price",
                    placeholder = "$19.99",
                    maxLength = 12,
                    style = RawTemplateTextStyleDefinition(
                        animation = "TYPEWRITER",
                    ),
                ),
                RawTextFieldDefinition(
                    id = "cta",
                    type = "CTA",
                    label = "CTA",
                    placeholder = "Shop Now",
                    maxLength = 18,
                    required = true,
                    style = RawTemplateTextStyleDefinition(
                        animation = "SLIDE_UP",
                        caseMode = "UPPERCASE",
                    ),
                ),
            ),
            timingStrategy = RawTimingStrategyDefinition.DistributedByCount(
                totalDurationMs = 9000L,
                minPerSlotMs = 900L,
                maxPerSlotMs = 1600L,
            ),
            defaultTransition = "ZOOM_IN",
            musicPolicy = RawTemplateMusicPolicyDefinition(
                selectionPolicy = "RECOMMENDED",
                preserveOriginalClipAudio = false,
                musicVolume = 1f,
            ),
            preview = RawTemplatePreviewDefinition(
                coverSource = "FIRST_MEDIA",
                previewVideoAssetName = "promo_flash_preview.mp4",
            ),
            isFeatured = true,
            sortOrder = 50,
        )
}