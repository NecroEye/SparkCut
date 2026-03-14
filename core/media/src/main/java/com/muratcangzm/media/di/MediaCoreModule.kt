package com.muratcangzm.media.di

import com.muratcangzm.media.data.AndroidMediaAssetResolver
import com.muratcangzm.media.data.AndroidMediaThumbnailProvider
import com.muratcangzm.media.data.export.AndroidAudioTrackDurationReader
import com.muratcangzm.media.data.export.AndroidAudioTrackMetadataReader
import com.muratcangzm.media.data.export.AndroidMedia3ExportEngine
import com.muratcangzm.media.data.export.AndroidMediaStoreExportFilePublisher
import com.muratcangzm.media.domain.MediaAssetResolver
import com.muratcangzm.media.domain.MediaThumbnailProvider
import com.muratcangzm.media.domain.export.AudioTrackDurationReader
import com.muratcangzm.media.domain.export.AudioTrackMetadataReader
import com.muratcangzm.media.domain.export.MediaExportEngine
import com.muratcangzm.media.domain.export.MediaExportFilePublisher
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val mediaCoreModule = module {
    single<MediaAssetResolver> {
        AndroidMediaAssetResolver(
            context = androidContext(),
        )
    }

    single<MediaThumbnailProvider> {
        AndroidMediaThumbnailProvider(
            context = androidContext(),
        )
    }

    single<AudioTrackMetadataReader> {
        AndroidAudioTrackMetadataReader(
            context = androidContext(),
        )
    }

    single<AudioTrackDurationReader> {
        AndroidAudioTrackDurationReader(
            context = androidContext(),
        )
    }

    single<MediaExportEngine> {
        AndroidMedia3ExportEngine(
            context = androidContext(),
            audioTrackDurationReader = get(),
        )
    }

    single<MediaExportFilePublisher> {
        AndroidMediaStoreExportFilePublisher(
            context = androidContext(),
        )
    }
}
