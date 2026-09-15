package com.example.iptvplayer.data.repository

import com.example.iptvplayer.data.database.AppDatabase
import com.example.iptvplayer.data.database.ChannelEntity
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.ChannelSortOption
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.domain.repository.ChannelRepository
import com.example.iptvplayer.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ChannelRepositoryImpl(
    private val database: AppDatabase,
    private val userRepository: UserRepository
) : ChannelRepository {

    private val channelDao = database.channelDao()

    override fun getChannels(playlistId: String?): Flow<List<Channel>> {
        return if (!playlistId.isNullOrBlank()) {
            getChannelsForPlaylist(playlistId)
        } else {
            val currentUserId = userRepository.activeUser.value?.id ?: ""
            getChannelsForUser(currentUserId)
        }
    }

    override fun getChannelsForPlaylist(playlistId: String): Flow<List<Channel>> {
        return channelDao.getChannelsForPlaylist(playlistId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getChannelsForUser(userId: String): Flow<List<Channel>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return channelDao.getChannelsForUser(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getFavoriteChannels(): Flow<List<Channel>> {
        val currentUserId = userRepository.activeUser.value?.id ?: ""
        return getFavoriteChannels(currentUserId)
    }

    override fun getFavoriteChannels(userId: String): Flow<List<Channel>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return channelDao.getFavoriteChannels(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getRecentChannels(): Flow<List<Channel>> {
        val currentUserId = userRepository.activeUser.value?.id ?: ""
        return getRecentChannels(currentUserId)
    }

    override fun getRecentChannels(userId: String): Flow<List<Channel>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return channelDao.getRecentChannels(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getDistinctCategories(userId: String, playlistId: String?): Flow<List<String>> {
        if (userId.isBlank()) return flowOf(emptyList())
        return if (!playlistId.isNullOrBlank()) {
            channelDao.getDistinctCategoriesForPlaylist(playlistId)
        } else {
            channelDao.getDistinctCategoriesForUser(userId)
        }.flowOn(Dispatchers.IO)
    }

    override fun getFilteredChannels(
        userId: String,
        playlistId: String?,
        category: String?,
        query: String,
        sort: ChannelSortOption,
        limit: Int
    ): Flow<List<Channel>> {
        if (userId.isBlank()) return flowOf(emptyList())
        val effectiveCategory = if (category.isNullOrBlank() || category.equals("Todos", ignoreCase = true)) {
            null
        } else {
            category
        }
        val cleanQuery = query.trim()
        val effectivePlaylistId = if (playlistId.isNullOrBlank()) null else playlistId
        return channelDao.getFilteredChannels(
            userId = userId,
            playlistId = effectivePlaylistId,
            groupTitle = effectiveCategory,
            query = cleanQuery,
            sort = sort.dbCode,
            limit = limit
        ).map { list -> list.map { it.toDomain() } }
        .flowOn(Dispatchers.IO)
    }

    override fun getFilteredChannelsCount(
        userId: String,
        playlistId: String?,
        category: String?,
        query: String
    ): Flow<Int> {
        if (userId.isBlank()) return flowOf(0)
        val effectiveCategory = if (category.isNullOrBlank() || category.equals("Todos", ignoreCase = true)) {
            null
        } else {
            category
        }
        val cleanQuery = query.trim()
        val effectivePlaylistId = if (playlistId.isNullOrBlank()) null else playlistId
        return channelDao.getFilteredChannelsCount(
            userId = userId,
            playlistId = effectivePlaylistId,
            groupTitle = effectiveCategory,
            query = cleanQuery
        ).flowOn(Dispatchers.IO)
    }

    override fun getFavoriteCount(userId: String): Flow<Int> {
        if (userId.isBlank()) return flowOf(0)
        return channelDao.getFavoriteCountForUser(userId).flowOn(Dispatchers.IO)
    }

    override suspend fun hasPlaybackHistory(userId: String): Boolean = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext false
        channelDao.getPlaybackHistoryCount(userId) > 0
    }

    override suspend fun markChannelPlayed(channelId: String) = withContext(Dispatchers.IO) {
        channelDao.incrementPlayCount(channelId)
    }

    override suspend fun getChannelCount(playlistId: String): Int = withContext(Dispatchers.IO) {
        channelDao.getChannelCountForPlaylist(playlistId)
    }

    override suspend fun getTotalUserChannelCount(userId: String): Int = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext 0
        channelDao.getChannelCountForUser(userId)
    }

    override suspend fun toggleFavorite(channelId: String) = withContext(Dispatchers.IO) {
        channelDao.toggleFavorite(channelId)
    }

    override suspend fun setFavorite(channelId: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        channelDao.setFavorite(channelId, isFavorite)
    }

    override suspend fun updateChannelStatus(
        channelId: String,
        status: StreamStatus,
        latencyMs: Long?
    ) = withContext(Dispatchers.IO) {
        channelDao.updateChannelStatus(
            channelId = channelId,
            status = status.name,
            latency = latencyMs,
            timestamp = System.currentTimeMillis()
        )
    }

    override suspend fun searchChannels(query: String): List<Channel> = withContext(Dispatchers.IO) {
        val currentUserId = userRepository.activeUser.value?.id ?: ""
        if (query.isBlank() || currentUserId.isBlank()) return@withContext emptyList()
        emptyList()
    }

    override fun searchChannelsFlow(userId: String, query: String): Flow<List<Channel>> {
        if (userId.isBlank() || query.isBlank()) return flowOf(emptyList())
        return channelDao.searchChannels(userId, query)
            .map { list -> list.map { it.toDomain() } }
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
