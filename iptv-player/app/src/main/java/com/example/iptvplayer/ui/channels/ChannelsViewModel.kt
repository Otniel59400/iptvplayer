package com.example.iptvplayer.ui.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.ChannelSortOption
import com.example.iptvplayer.domain.model.Playlist
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.domain.repository.ChannelRepository
import com.example.iptvplayer.domain.repository.PlaylistRepository
import com.example.iptvplayer.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class ChannelsViewModel(
    private val channelRepository: ChannelRepository,
    private val playlistRepository: PlaylistRepository,
    private val userRepository: UserRepository
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

    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    private val _selectedCategory = MutableStateFlow("Todos")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val debouncedSearch = _searchQuery
        .debounce(250)
        .distinctUntilChanged()

    private val _sortOption = MutableStateFlow(ChannelSortOption.NAME_ASC)
    val sortOption: StateFlow<ChannelSortOption> = _sortOption.asStateFlow()

    private val _hasHistory = MutableStateFlow(false)
    val hasHistory: StateFlow<Boolean> = _hasHistory.asStateFlow()

    private val _limit = MutableStateFlow(120)
    val limit: StateFlow<Int> = _limit.asStateFlow()

    private val _selectedChannelForDetails = MutableStateFlow<Channel?>(null)
    val selectedChannelForDetails: StateFlow<Channel?> = _selectedChannelForDetails.asStateFlow()

    init {
        // Automatically select single playlist if only 1 exists and none is chosen
        viewModelScope.launch {
            playlists.collect { list ->
                if (_selectedPlaylistId.value == null && list.size == 1) {
                    _selectedPlaylistId.value = list.first().id
                }
                checkHistory()
            }
        }
    }

    val categories: StateFlow<List<String>> = combine(
        activeUser,
        _selectedPlaylistId
    ) { user, playlistId ->
        user?.id to playlistId
    }.flatMapLatest { (userId, playlistId) ->
        if (userId.isNullOrBlank()) {
            flowOf(listOf("Todos"))
        } else {
            channelRepository.getDistinctCategories(userId, playlistId)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = listOf("Todos")
    )

    private data class FilterConfig(
        val category: String,
        val query: String,
        val sort: ChannelSortOption,
        val limit: Int
    )

    private val filterConfig = combine(
        _selectedCategory,
        debouncedSearch,
        _sortOption,
        _limit
    ) { category, query, sort, currentLimit ->
        FilterConfig(category, query, sort, currentLimit)
    }

    val channels: StateFlow<List<Channel>> = combine(
        activeUser,
        _selectedPlaylistId,
        filterConfig
    ) { user, playlistId, config ->
        Triple(user?.id.orEmpty(), playlistId, config)
    }.flatMapLatest { (userId, playlistId, config) ->
        if (userId.isBlank()) {
            flowOf(emptyList())
        } else {
            channelRepository.getFilteredChannels(
                userId = userId,
                playlistId = playlistId,
                category = config.category,
                query = config.query,
                sort = config.sort,
                limit = config.limit
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val filteredCount: StateFlow<Int> = combine(
        activeUser,
        _selectedPlaylistId,
        _selectedCategory,
        debouncedSearch
    ) { user, playlistId, category, query ->
        val userId = user?.id.orEmpty()
        if (userId.isBlank()) {
            flowOf(0)
        } else {
            channelRepository.getFilteredChannelsCount(
                userId = userId,
                playlistId = playlistId,
                category = category,
                query = query
            )
        }
    }.flatMapLatest { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    val totalCount: StateFlow<Int> = combine(
        activeUser,
        _selectedPlaylistId
    ) { user, playlistId ->
        val userId = user?.id.orEmpty()
        if (userId.isBlank()) {
            flowOf(0)
        } else {
            channelRepository.getFilteredChannelsCount(
                userId = userId,
                playlistId = playlistId,
                category = null,
                query = ""
            )
        }
    }.flatMapLatest { it }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    fun setSelectedPlaylist(playlistId: String?) {
        _selectedPlaylistId.value = playlistId
        _selectedCategory.value = "Todos"
        _limit.value = 120
        checkHistory()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        _limit.value = 120
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
        _limit.value = 120
    }

    fun setSortOption(option: ChannelSortOption) {
        _sortOption.value = option
    }

    fun openChannelDetails(channel: Channel) {
        _selectedChannelForDetails.value = channel
    }

    fun closeChannelDetails() {
        _selectedChannelForDetails.value = null
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            channelRepository.toggleFavorite(channelId)
            val current = _selectedChannelForDetails.value
            if (current?.id == channelId) {
                _selectedChannelForDetails.value = current.copy(isFavorite = !current.isFavorite)
            }
        }
    }

    fun markChannelPlayed(channelId: String) {
        viewModelScope.launch {
            channelRepository.markChannelPlayed(channelId)
            checkHistory()
        }
    }

    fun loadMore() {
        _limit.value += 100
    }

    private fun checkHistory() {
        val userId = activeUser.value?.id ?: return
        viewModelScope.launch {
            _hasHistory.value = channelRepository.hasPlaybackHistory(userId)
            if (!_hasHistory.value && (_sortOption.value == ChannelSortOption.RECENT || _sortOption.value == ChannelSortOption.MOST_PLAYED)) {
                _sortOption.value = ChannelSortOption.NAME_ASC
            }
        }
    }

    class Factory(
        private val channelRepository: ChannelRepository,
        private val playlistRepository: PlaylistRepository,
        private val userRepository: UserRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChannelsViewModel(
                channelRepository = channelRepository,
                playlistRepository = playlistRepository,
                userRepository = userRepository
            ) as T
        }
    }
}
