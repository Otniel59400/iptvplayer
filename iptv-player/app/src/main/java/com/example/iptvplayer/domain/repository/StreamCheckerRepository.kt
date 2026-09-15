package com.example.iptvplayer.domain.repository

import com.example.iptvplayer.data.checker.CheckerProgressState
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.StreamCheckReport
import com.example.iptvplayer.domain.model.StreamStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface StreamCheckerRepository {
    val progressState: StateFlow<CheckerProgressState>
    fun startVerification(playlistId: String, playlistName: String, maxConcurrency: Int = 3)
    fun stopVerification()
    fun getChannelCountByStatus(playlistId: String, status: StreamStatus): Flow<Int>
    fun getTotalChannelsForPlaylist(playlistId: String): Flow<Int>
    fun getCheckerChannels(
        userId: String,
        playlistId: String,
        statusFilter: StreamStatus?,
        sort: String,
        limit: Int = 200
    ): Flow<List<Channel>>
    fun getCheckerChannelsCount(
        userId: String,
        playlistId: String,
        statusFilter: StreamStatus?
    ): Flow<Int>
    fun getChannelCountByStatusForUser(userId: String, status: StreamStatus): Flow<Int>
    fun getVerifiedCountForUser(userId: String): Flow<Int>
}
