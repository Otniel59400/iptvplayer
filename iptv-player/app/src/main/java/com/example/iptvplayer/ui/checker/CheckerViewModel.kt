package com.example.iptvplayer.ui.checker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.iptvplayer.data.checker.CheckerProgressState
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.Playlist
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.domain.repository.PlaylistRepository
import com.example.iptvplayer.domain.repository.StreamCheckerRepository
import com.example.iptvplayer.domain.repository.UserRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class CheckerSort(val labelPt: String, val dbCode: String) {
    NAME_ASC("Nome A → Z", "NAME_ASC"),
    NAME_DESC("Nome Z → A", "NAME_DESC"),
    STATUS("Estado", "STATUS"),
    LATENCY("Latência", "LATENCY"),
    LAST_CHECKED("Última verificação", "LAST_CHECKED")
}

data class CheckerStats(
    val total: Int = 0,
    val online: Int = 0,
    val offline: Int = 0,
    val checking: Int = 0,
    val indeterminate: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class CheckerViewModel(
    private val checkerRepository: StreamCheckerRepository,
    private val playlistRepository: PlaylistRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    val activeUser = userRepository.activeUser

    // Current user's playlists
    val userPlaylists: StateFlow<List<Playlist>> = activeUser
        .flatMapLatest { user: User? ->
            if (user != null) {
                playlistRepository.getPlaylists(user.id)
            } else {
                flowOf(emptyList<Playlist>())
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected playlist ID
    private val _selectedPlaylistId = MutableStateFlow<String?>(null)
    val selectedPlaylistId: StateFlow<String?> = _selectedPlaylistId.asStateFlow()

    // Status filter
    private val _statusFilter = MutableStateFlow<StreamStatus?>(null)
    val statusFilter: StateFlow<StreamStatus?> = _statusFilter.asStateFlow()

    // Sort option
    private val _sortOption = MutableStateFlow(CheckerSort.NAME_ASC)
    val sortOption: StateFlow<CheckerSort> = _sortOption.asStateFlow()

    // Pagination limit
    private val _limit = MutableStateFlow(100)
    val limit: StateFlow<Int> = _limit.asStateFlow()

    // Progress State from repository/engine
    val progressState: StateFlow<CheckerProgressState> = checkerRepository.progressState

    // Auto-select first playlist when loaded if not selected yet
    init {
        viewModelScope.launch {
            userPlaylists.collect { playlists ->
                if (_selectedPlaylistId.value == null && playlists.isNotEmpty()) {
                    _selectedPlaylistId.value = playlists.first().id
                } else if (_selectedPlaylistId.value != null && playlists.none { it.id == _selectedPlaylistId.value }) {
                    _selectedPlaylistId.value = playlists.firstOrNull()?.id
                }
            }
        }
    }

    fun selectPlaylist(playlistId: String) {
        _selectedPlaylistId.value = playlistId
    }

    fun setStatusFilter(status: StreamStatus?) {
        _statusFilter.value = status
    }

    fun setSortOption(sort: CheckerSort) {
        _sortOption.value = sort
    }

    fun loadMoreChannels() {
        _limit.value += 100
    }

    // Real database counts for selected playlist
    val stats: StateFlow<CheckerStats> = _selectedPlaylistId
        .flatMapLatest { playlistId ->
            if (playlistId == null) {
                flowOf(CheckerStats())
            } else {
                combine(
                    checkerRepository.getChannelCountByStatus(playlistId, StreamStatus.ONLINE),
                    checkerRepository.getChannelCountByStatus(playlistId, StreamStatus.OFFLINE),
                    checkerRepository.getChannelCountByStatus(playlistId, StreamStatus.CHECKING),
                    checkerRepository.getChannelCountByStatus(playlistId, StreamStatus.UNKNOWN)
                ) { online, offline, checking, unknown ->
                    val total = online + offline + checking + unknown
                    CheckerStats(
                        total = total,
                        online = online,
                        offline = offline,
                        checking = checking,
                        indeterminate = unknown
                    )
                }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), CheckerStats())

    // Observable filtered channels list from real Room database
    val channels: StateFlow<List<Channel>> = combine(
        activeUser,
        _selectedPlaylistId,
        _statusFilter,
        _sortOption,
        _limit
    ) { user, playlistId, filter, sort, lim ->
        Triple(user, playlistId, Triple(filter, sort, lim))
    }.flatMapLatest { (user, playlistId, tuple) ->
        val (filter, sort, lim) = tuple
        if (user == null || playlistId == null) {
            flowOf(emptyList())
        } else {
            checkerRepository.getCheckerChannels(
                userId = user.id,
                playlistId = playlistId,
                statusFilter = filter,
                sort = sort.dbCode,
                limit = lim
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Total filtered count
    val totalFilteredCount: StateFlow<Int> = combine(
        activeUser,
        _selectedPlaylistId,
        _statusFilter
    ) { user, playlistId, filter ->
        Pair(user, Pair(playlistId, filter))
    }.flatMapLatest { (user, pair) ->
        val (playlistId, filter) = pair
        if (user == null || playlistId == null) {
            flowOf(0)
        } else {
            checkerRepository.getCheckerChannelsCount(
                userId = user.id,
                playlistId = playlistId,
                statusFilter = filter
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun startVerification() {
        val playlistId = _selectedPlaylistId.value ?: return
        val playlist = userPlaylists.value.find { it.id == playlistId }
        val playlistName = playlist?.name ?: "Playlist"
        checkerRepository.startVerification(
            playlistId = playlistId,
            playlistName = playlistName,
            maxConcurrency = 3
        )
    }

    fun stopVerification() {
        checkerRepository.stopVerification()
    }

    fun recheck() {
        startVerification()
    }

    companion object {
        fun provideFactory(
            checkerRepository: StreamCheckerRepository,
            playlistRepository: PlaylistRepository,
            userRepository: UserRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CheckerViewModel(
                    checkerRepository = checkerRepository,
                    playlistRepository = playlistRepository,
                    userRepository = userRepository
                ) as T
            }
        }
    }
}
