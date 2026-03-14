package com.muratcangzm.media.domain

object MediaDurationFormatter {

    fun format(durationMs: Long?): String {
        if (durationMs == null || durationMs <= 0L) return "0:00"

        val totalSeconds = durationMs / 1000L
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}