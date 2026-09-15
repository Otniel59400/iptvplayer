package com.example.iptvplayer.ui.player

import android.app.Application
import android.net.Uri
import androidx.annotation.OptIn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.ChannelSortOption
import com.example.iptvplayer.domain.repository.ChannelRepository
import com.example.iptvplayer.domain.repository.PlaylistRepository
import com.example.iptvplayer.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@OptIn(UnstableApi::class)
class PlayerViewModel(
    application: Application,
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val playlistRepository: PlaylistRepository
) : AndroidViewModel(application) {

    companion object {
        const val MAX_RECONNECT_ATTEMPTS = 3
        const val CONTROLS_AUTO_HIDE_MS = 5000L
        const val MAX_ACCEPTABLE_LIVE_DRIFT_MS = 30000L // 30 seconds

        fun provideFactory(
            application: Application,
            channelRepository: ChannelRepository,
            userRepository: UserRepository,
            playlistRepository: PlaylistRepository
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                PlayerViewModel(
                    application = application,
                    channelRepository = channelRepository,
                    userRepository = userRepository,
                    playlistRepository = playlistRepository
                )
            }
        }
    }

    // Media3 ExoPlayer instance optimized for HY300 hardware constraints
    val exoPlayer: ExoPlayer by lazy {
        val context = getApplication<Application>()

        // Low memory, low CPU buffering load control for IPTV streams
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ 15000,
                /* maxBufferMs = */ 30000,
                /* bufferForPlaybackMs = */ 2500,
                /* bufferForPlaybackAfterRebufferMs = */ 4000
            )
            .setPrioritizeTimeOverSizeThresholds(true)
            .build()

        // Restrict video resolution to 1080p max to avoid decoder stuttering on HY300
        val trackSelector = DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setMaxVideoSize(1920, 1080)
                    .setMaxVideoFrameRate(60)
            )
        }

        // Hardware decoders only
        val renderersFactory = DefaultRenderersFactory(context)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

        val mediaSourceFactory = DefaultMediaSourceFactory(context)

        ExoPlayer.Builder(context, renderersFactory)
            .setMediaSourceFactory(mediaSourceFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .build().apply {
                addListener(playerListener)
                playWhenReady = true
            }
    }

    // Current playing channel
    private val _currentChannel = MutableStateFlow<Channel?>(null)
    val currentChannel: StateFlow<Channel?> = _currentChannel.asStateFlow()

    // Playback state
    private val _playbackState = MutableStateFlow<PlayerPlaybackState>(PlayerPlaybackState.Idle)
    val playbackState: StateFlow<PlayerPlaybackState> = _playbackState.asStateFlow()

    // Controls visibility
    private val _isControlsVisible = MutableStateFlow(true)
    val isControlsVisible: StateFlow<Boolean> = _isControlsVisible.asStateFlow()

    // Channel list overlay
    private val _isChannelOverlayOpen = MutableStateFlow(false)
    val isChannelOverlayOpen: StateFlow<Boolean> = _isChannelOverlayOpen.asStateFlow()

    // Volume & Mute
    private val _isMuted = MutableStateFlow(false)
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _volume = MutableStateFlow(1.0f)
    val volume: StateFlow<Float> = _volume.asStateFlow()

    // Aspect ratio: true = Fit (16:9), false = Zoom/Fill
    private val _isAspectRatioFit = MutableStateFlow(true)
    val isAspectRatioFit: StateFlow<Boolean> = _isAspectRatioFit.asStateFlow()

    // Live stream detection & live offset delay
    // null: not a live stream; -1: live stream but delay unknown; > 0: delay in milliseconds
    private val _liveOffsetMs = MutableStateFlow<Long?>(null)
    val liveOffsetMs: StateFlow<Long?> = _liveOffsetMs.asStateFlow()

    private val _isLiveStream = MutableStateFlow(false)
    val isLiveStream: StateFlow<Boolean> = _isLiveStream.asStateFlow()

    // Favorites
    private val _isFavorite = MutableStateFlow(false)
    val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

    // Reconnection tracking
    private var reconnectAttempts = 0
    private var reconnectJob: Job? = null
    private var controlsHideJob: Job? = null
    private var liveMonitoringJob: Job? = null
    private var hasMarkedHistoryForCurrentChannel = false

    // Overlay channels and search/filter state
    private val _overlaySearchQuery = MutableStateFlow("")
    val overlaySearchQuery: StateFlow<String> = _overlaySearchQuery.asStateFlow()

    private val _selectedOverlayCategory = MutableStateFlow<String?>("Todos")
    val selectedOverlayCategory: StateFlow<String?> = _selectedOverlayCategory.asStateFlow()

    private val _activePlaylistId = MutableStateFlow<String?>(null)
    val activePlaylistId: StateFlow<String?> = _activePlaylistId.asStateFlow()

    // Overlay Channels Flow
    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    val overlayChannels: StateFlow<List<Channel>> = combine(
        _activePlaylistId,
        _selectedOverlayCategory,
        _overlaySearchQuery,
        userRepository.activeUser
    ) { playlistId, category, query, activeUser ->
        val userId = activeUser?.id ?: ""
        Triple(userId, playlistId to category, query)
    }.flatMapLatest { (userId, playlistAndCategory, query) ->
        val (playlistId, category) = playlistAndCategory
        if (userId.isBlank()) {
            flowOf(emptyList())
        } else {
            channelRepository.getFilteredChannels(
                userId = userId,
                playlistId = playlistId,
                category = if (category == "Todos") null else category,
                query = query,
                sort = ChannelSortOption.NAME_ASC,
                limit = 200
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Overlay Categories Flow
    @kotlin.OptIn(ExperimentalCoroutinesApi::class)
    val overlayCategories: StateFlow<List<String>> = combine(
        _activePlaylistId,
        userRepository.activeUser
    ) { playlistId, activeUser ->
        val userId = activeUser?.id ?: ""
        userId to playlistId
    }.flatMapLatest { (userId, playlistId) ->
        if (userId.isBlank()) {
            flowOf(listOf("Todos"))
        } else {
            channelRepository.getDistinctCategories(userId, playlistId).map { list ->
                if ("Todos" in list) list else listOf("Todos") + list
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Todos")
    )

    private val playerListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            when (state) {
                Player.STATE_BUFFERING -> {
                    if (_playbackState.value !is PlayerPlaybackState.Reconnecting) {
                        _playbackState.value = PlayerPlaybackState.Buffering()
                    }
                }
                Player.STATE_READY -> {
                    reconnectAttempts = 0
                    if (exoPlayer.playWhenReady) {
                        _playbackState.value = PlayerPlaybackState.Playing
                        onPlaybackSuccess()
                    } else {
                        _playbackState.value = PlayerPlaybackState.Paused
                    }
                }
                Player.STATE_ENDED -> {
                    _playbackState.value = PlayerPlaybackState.Paused
                }
                Player.STATE_IDLE -> {
                    if (_playbackState.value !is PlayerPlaybackState.Error &&
                        _playbackState.value !is PlayerPlaybackState.Reconnecting
                    ) {
                        _playbackState.value = PlayerPlaybackState.Idle
                    }
                }
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (isPlaying) {
                _playbackState.value = PlayerPlaybackState.Playing
                onPlaybackSuccess()
            } else if (exoPlayer.playbackState == Player.STATE_READY) {
                _playbackState.value = PlayerPlaybackState.Paused
            }
        }

        override fun onPlayerError(error: PlaybackException) {
            handlePlaybackFailure(error)
        }
    }

    init {
        startLiveMonitoring()
    }

    /**
     * Start playing a channel. Plays the real stream URL.
     * Never uses fake demo videos.
     */
    fun playChannel(channel: Channel, playlistId: String? = null) {
        _currentChannel.value = channel
        _isFavorite.value = channel.isFavorite
        _activePlaylistId.value = playlistId ?: channel.playlistId.ifBlank { null }
        hasMarkedHistoryForCurrentChannel = false
        reconnectAttempts = 0
        reconnectJob?.cancel()

        _playbackState.value = PlayerPlaybackState.Loading("Carregando…")

        // Build real MediaItem with adaptive format detection
        val mediaItem = buildMediaItem(channel)

        try {
            exoPlayer.stop()
            exoPlayer.clearMediaItems()
            exoPlayer.setMediaItem(mediaItem)
            exoPlayer.prepare()
            exoPlayer.play()
        } catch (e: Exception) {
            handlePlaybackFailure(e)
        }

        // Show controls initially, schedule auto-hide
        showControls()
    }

    private fun buildMediaItem(channel: Channel): MediaItem {
        val uri = Uri.parse(channel.streamUrl)
        val urlString = channel.streamUrl.lowercase()

        val mimeType = when {
            urlString.contains(".m3u8") || urlString.contains("m3u8") -> MimeTypes.APPLICATION_M3U8
            urlString.contains(".mpd") -> MimeTypes.APPLICATION_MPD
            urlString.contains(".ism") -> MimeTypes.APPLICATION_SS
            else -> null // Auto-detect progressive HTTP or container
        }

        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(channel.name)
            .setArtist(channel.category)

        channel.logoUrl?.let { logo ->
            if (logo.isNotBlank()) {
                metadataBuilder.setArtworkUri(Uri.parse(logo))
            }
        }

        val liveConfig = MediaItem.LiveConfiguration.Builder()
            .setMaxPlaybackSpeed(1.03f)
            .setMinPlaybackSpeed(0.97f)
            .setTargetOffsetMs(3500L)
            .build()

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(uri)
            .setMediaMetadata(metadataBuilder.build())
            .setLiveConfiguration(liveConfig)

        if (mimeType != null) {
            mediaItemBuilder.setMimeType(mimeType)
        }

        return mediaItemBuilder.build()
    }

    /**
     * Called when stream starts playing successfully.
     * Records playback history and updates last played timestamp in Room.
     */
    private fun onPlaybackSuccess() {
        val channel = _currentChannel.value ?: return
        if (!hasMarkedHistoryForCurrentChannel) {
            hasMarkedHistoryForCurrentChannel = true
            viewModelScope.launch {
                try {
                    channelRepository.markChannelPlayed(channel.id)
                } catch (_: Exception) {
                }
            }
        }
    }

    /**
     * Controlled automatic reconnection mechanism.
     * Prevents infinite reconnect loops with reasonable attempt threshold.
     */
    private fun handlePlaybackFailure(error: Throwable) {
        if (reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
            reconnectAttempts++
            _playbackState.value = PlayerPlaybackState.Reconnecting(
                attempt = reconnectAttempts,
                maxAttempts = MAX_RECONNECT_ATTEMPTS
            )

            reconnectJob?.cancel()
            reconnectJob = viewModelScope.launch {
                delay(2000L) // Wait 2s before retry
                val channel = _currentChannel.value
                if (channel != null && isActive) {
                    try {
                        val mediaItem = buildMediaItem(channel)
                        exoPlayer.setMediaItem(mediaItem)
                        exoPlayer.prepare()
                        exoPlayer.play()
                    } catch (e: Exception) {
                        handlePlaybackFailure(e)
                    }
                }
            }
        } else {
            _playbackState.value = PlayerPlaybackState.Error("Não foi possível reproduzir este canal.")
            showControls()
        }
    }

    /**
     * Retry playing the current channel manually.
     */
    fun retry() {
        val channel = _currentChannel.value ?: return
        reconnectAttempts = 0
        reconnectJob?.cancel()
        playChannel(channel, _activePlaylistId.value)
    }

    /**
     * Live playback position and live edge offset monitoring.
     * Recovers from excessive drift and updates real delay display.
     */
    private fun startLiveMonitoring() {
        liveMonitoringJob?.cancel()
        liveMonitoringJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                try {
                    val isLive = exoPlayer.isCurrentMediaItemLive
                    _isLiveStream.value = isLive

                    if (isLive) {
                        val currentOffset = exoPlayer.currentLiveOffset
                        if (currentOffset != C.TIME_UNSET && currentOffset >= 0) {
                            _liveOffsetMs.value = currentOffset

                            // Recover from severe live drift without jarring jump
                            if (currentOffset > MAX_ACCEPTABLE_LIVE_DRIFT_MS) {
                                exoPlayer.seekToDefaultPosition()
                            }
                        } else {
                            // Live, but delay cannot be measured reliably
                            _liveOffsetMs.value = -1L
                        }
                    } else {
                        _liveOffsetMs.value = null
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    // Toggle playback
    fun togglePlayPause() {
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
            _playbackState.value = PlayerPlaybackState.Paused
            showControls()
        } else {
            exoPlayer.play()
            _playbackState.value = PlayerPlaybackState.Playing
            userInteracted()
        }
    }

    // Toggle mute
    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        exoPlayer.volume = if (newMuted) 0f else _volume.value
        userInteracted()
    }

    // Set volume level
    fun setVolume(newVolume: Float) {
        val clamped = newVolume.coerceIn(0f, 1f)
        _volume.value = clamped
        if (!_isMuted.value) {
            exoPlayer.volume = clamped
        }
        userInteracted()
    }

    // Toggle aspect ratio (Fit 16:9 vs Zoom/Fill)
    fun toggleAspectRatio() {
        _isAspectRatioFit.value = !_isAspectRatioFit.value
        userInteracted()
    }

    // Toggle favorite directly from player controls
    fun toggleFavorite() {
        val channel = _currentChannel.value ?: return
        val newFav = !_isFavorite.value
        _isFavorite.value = newFav
        _currentChannel.value = channel.copy(isFavorite = newFav)

        viewModelScope.launch {
            try {
                channelRepository.setFavorite(channel.id, newFav)
            } catch (_: Exception) {
            }
        }
        userInteracted()
    }

    // Controls visibility & auto-hide
    fun showControls() {
        _isControlsVisible.value = true
        resetAutoHideTimer()
    }

    fun hideControls() {
        _isControlsVisible.value = false
        controlsHideJob?.cancel()
    }

    fun toggleControls() {
        if (_isControlsVisible.value) {
            hideControls()
        } else {
            showControls()
        }
    }

    fun userInteracted() {
        if (_isControlsVisible.value) {
            resetAutoHideTimer()
        }
    }

    private fun resetAutoHideTimer() {
        controlsHideJob?.cancel()
        // Do not auto hide if overlay is open or in error state
        if (_isChannelOverlayOpen.value || _playbackState.value is PlayerPlaybackState.Error) {
            return
        }
        controlsHideJob = viewModelScope.launch {
            delay(CONTROLS_AUTO_HIDE_MS)
            if (!_isChannelOverlayOpen.value && _playbackState.value !is PlayerPlaybackState.Error) {
                _isControlsVisible.value = false
            }
        }
    }

    // Channel Overlay controls
    fun openChannelOverlay() {
        _isChannelOverlayOpen.value = true
        _isControlsVisible.value = true
        controlsHideJob?.cancel()
    }

    fun closeChannelOverlay() {
        _isChannelOverlayOpen.value = false
        resetAutoHideTimer()
    }

    fun toggleChannelOverlay() {
        if (_isChannelOverlayOpen.value) {
            closeChannelOverlay()
        } else {
            openChannelOverlay()
        }
    }

    fun setOverlaySearchQuery(query: String) {
        _overlaySearchQuery.value = query
    }

    fun selectOverlayCategory(category: String?) {
        _selectedOverlayCategory.value = category ?: "Todos"
    }

    /**
     * Switch channel directly inside player without returning to Home or playlist screen.
     */
    fun selectChannelFromOverlay(channel: Channel) {
        closeChannelOverlay()
        playChannel(channel, _activePlaylistId.value)
    }

    override fun onCleared() {
        super.onCleared()
        liveMonitoringJob?.cancel()
        reconnectJob?.cancel()
        controlsHideJob?.cancel()
        try {
            exoPlayer.removeListener(playerListener)
            exoPlayer.release()
        } catch (_: Exception) {
        }
    }
}
