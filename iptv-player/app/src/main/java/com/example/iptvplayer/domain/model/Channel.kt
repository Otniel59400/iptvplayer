package com.example.iptvplayer.domain.model

enum class StreamStatus {
    ONLINE,
    OFFLINE,
    CHECKING,
    UNKNOWN;

    val labelPt: String
        get() = when (this) {
            ONLINE -> "Online"
            OFFLINE -> "Offline"
            CHECKING -> "A verificar"
            UNKNOWN -> "Indeterminado"
        }

    val symbol: String
        get() = when (this) {
            ONLINE -> "🟢"
            OFFLINE -> "🔴"
            CHECKING -> "🟡"
            UNKNOWN -> "⚪"
        }
}

enum class ChannelSortOption(val labelPt: String, val dbCode: String) {
    NAME_ASC("Nome A → Z", "NAME_ASC"),
    NAME_DESC("Nome Z → A", "NAME_DESC"),
    CATEGORY("Categoria", "CATEGORY"),
    RECENT("Mais recentes", "RECENT"),
    MOST_PLAYED("Mais vistos", "MOST_PLAYED")
}

data class Channel(
    val id: String,
    val playlistId: String = "",
    val userId: String = "",
    val name: String,
    val category: String = "Geral",
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String? = category,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val isFavorite: Boolean = false,
    val status: StreamStatus = StreamStatus.UNKNOWN,
    val latencyMs: Long? = null,
    val lastCheckedTimestamp: Long? = null,
    val orderIndex: Int = 0,
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0
)
