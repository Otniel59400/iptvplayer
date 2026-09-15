package com.example.iptvplayer.data.repository

import com.example.iptvplayer.data.checker.CheckerProgressState
import com.example.iptvplayer.data.checker.StreamCheckerEngine
import com.example.iptvplayer.data.database.AppDatabase
import com.example.iptvplayer.data.database.ChannelEntity
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.domain.repository.StreamCheckerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class StreamCheckerRepositoryImpl(
    private val database: AppDatabase,
    private val engine: StreamCheckerEngine = StreamCheckerEngine(database)
) : StreamCheckerRepository {

    private val channelDao = database.channelDao()

    override val progressState: StateFlow<CheckerProgressState> = engine.progressState

    override fun startVerification(playlistId: String, playlistName: String, maxConcurrency: Int) {
        engine.startVerification(playlistId, playlistName, maxConcurrency)
    }

    override fun stopVerification() {
        engine.stopVerification()
    }

    override fun getChannelCountByStatus(playlistId: String, status: StreamStatus): Flow<Int> {
        return channelDao.getChannelCountByStatusForPlaylist(playlistId, status.name)
            .flowOn(Dispatchers.IO)
    }

    override fun getTotalChannelsForPlaylist(playlistId: String): Flow<Int> {
        return channelDao.getCheckerChannelsCount(
            userId = "",
            playlistId = playlistId,
            statusFilter = null
        ).flowOn(Dispatchers.IO)
    }

    override fun getCheckerChannels(
        userId: String,
        playlistId: String,
        statusFilter: StreamStatus?,
        sort: String,
        limit: Int
    ): Flow<List<Channel>> {
        return channelDao.getCheckerChannels(
            userId = userId,
            playlistId = playlistId,
            statusFilter = statusFilter?.name,
            sort = sort,
            limit = limit
        ).map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getCheckerChannelsCount(
        userId: String,
        playlistId: String,
        statusFilter: StreamStatus?
    ): Flow<Int> {
        return channelDao.getCheckerChannelsCount(
            userId = userId,
            playlistId = playlistId,
            statusFilter = statusFilter?.name
        ).flowOn(Dispatchers.IO)
    }

    override fun getChannelCountByStatusForUser(userId: String, status: StreamStatus): Flow<Int> {
        return channelDao.getChannelCountByStatusForUser(userId, status.name)
            .flowOn(Dispatchers.IO)
    }

    override fun getVerifiedCountForUser(userId: String): Flow<Int> {
        return channelDao.getVerifiedChannelCountForUser(userId)
            .flowOn(Dispatchers.IO)
    }

    private fun ChannelEntity.toDomain(): Channel {
        val streamStatus = try {
            StreamStatus.valueOf(status)
        } catch (_: Exception) {
            StreamStatus.UNKNOWN
        }
        val group = groupTitle?.ifBlank { "Geral" } ?: "Geral"
        return Channel(
            id = id,
            playlistId = playlistId,
            userId = userId,
            name = name,
            category = group,
            groupTitle = group,
            streamUrl = streamUrl,
            logoUrl = logoUrl,
            tvgId = tvgId,
            tvgName = tvgName,
            status = streamStatus,
            latencyMs = latency,
            lastCheckedTimestamp = lastChecked,
            orderIndex = orderIndex,
            isFavorite = isFavorite,
            lastPlayedAt = lastPlayedAt,
            playCount = playCount
        )
    }
}


