package com.example.iptvplayer.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.Playlist
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.domain.repository.ChannelRepository
import com.example.iptvplayer.domain.repository.PlaylistRepository
import com.example.iptvplayer.domain.repository.StreamCheckerRepository
import com.example.iptvplayer.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for the IPTV Home Dashboard.
 * Strictly guarantees user isolation by observing [activeUser] and
 * switching Room flows whenever the authenticated profile changes.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val playlistRepository: PlaylistRepository,
    private val channelRepository: ChannelRepository,
    private val userRepository: UserRepository,
    private val streamCheckerRepository: StreamCheckerRepository
) : ViewModel() {

    val activeUser: StateFlow<User?> = userRepository.activeUser

    val playlists: StateFlow<List<Playlist>> = activeUser.flatMapLatest { user ->
        if (user != null) {
            playlistRepository.getPlaylists(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoriteChannels: StateFlow<List<Channel>> = activeUser.flatMapLatest { user ->
        if (user != null) {
            channelRepository.getFavoriteChannels(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentChannels: StateFlow<List<Channel>> = activeUser.flatMapLatest { user ->
        if (user != null) {
            channelRepository.getRecentChannels(user.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Real database channel verification counts for current user
    val onlineCount: StateFlow<Int> = activeUser.flatMapLatest { user ->
        if (user != null) {
            streamCheckerRepository.getChannelCountByStatusForUser(user.id, StreamStatus.ONLINE)
        } else {
            flowOf(0)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val offlineCount: StateFlow<Int> = activeUser.flatMapLatest { user ->
        if (user != null) {
            streamCheckerRepository.getChannelCountByStatusForUser(user.id, StreamStatus.OFFLINE)
        } else {
            flowOf(0)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val verifiedCount: StateFlow<Int> = activeUser.flatMapLatest { user ->
        if (user != null) {
            streamCheckerRepository.getVerifiedCountForUser(user.id)
        } else {
            flowOf(0)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId)
        }
    }

    fun markChannelPlayed(channelId: String) {
        viewModelScope.launch {
            channelRepository.markChannelPlayed(channelId)
        }
    }

    companion object {
        fun provideFactory(
            playlistRepository: PlaylistRepository,
            channelRepository: ChannelRepository,
            userRepository: UserRepository,
            streamCheckerRepository: StreamCheckerRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    playlistRepository = playlistRepository,
                    channelRepository = channelRepository,
                    userRepository = userRepository,
                    streamCheckerRepository = streamCheckerRepository
                ) as T
            }
        }
    }
}
