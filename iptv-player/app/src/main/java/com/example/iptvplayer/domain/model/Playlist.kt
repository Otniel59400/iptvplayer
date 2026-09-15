package com.example.iptvplayer.domain.model

enum class PlaylistType {
    LOCAL,
    URL,
    CUSTOM
}

data class Playlist(
    val id: String,
    val userId: String = "",
    val name: String,
    val type: PlaylistType = PlaylistType.LOCAL,
    val source: String = "",
    val url: String? = null,
    val channelCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSync: Long? = null,
    val orderIndex: Int = 0,
    val isCustom: Boolean = false
) {
    val isLocalFile: Boolean get() = type == PlaylistType.LOCAL
    val sourceUrl: String? get() = url ?: source.ifBlank { null }
}

