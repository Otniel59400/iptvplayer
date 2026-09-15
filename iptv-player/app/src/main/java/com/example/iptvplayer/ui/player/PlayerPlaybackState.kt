package com.example.iptvplayer.ui.player

/**
 * Playback states for the Real Media3 / ExoPlayer IPTV player.
 */
sealed interface PlayerPlaybackState {
    object Idle : PlayerPlaybackState
    data class Loading(val message: String = "Carregando…") : PlayerPlaybackState
    data class Buffering(val message: String = "Buffering…") : PlayerPlaybackState
    object Playing : PlayerPlaybackState
    object Paused : PlayerPlaybackState
    data class Reconnecting(val attempt: Int, val maxAttempts: Int = 3) : PlayerPlaybackState
    data class Error(val message: String = "Não foi possível reproduzir este canal.") : PlayerPlaybackState
}
