package com.example.iptvplayer.domain.model

data class StreamCheckReport(
    val channelId: String,
    val channelName: String,
    val status: StreamStatus,
    val latencyMs: Long,
    val checkedAt: Long = System.currentTimeMillis(),
    val notes: String? = null
)
