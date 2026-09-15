package com.example.iptvplayer.domain.repository

import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.ChannelSortOption
import com.example.iptvplayer.domain.model.StreamStatus
import kotlinx.coroutines.flow.Flow

interface ChannelRepository {
    fun getChannels(playlistId: String? = null): Flow<List<Channel>>
    fun getChannelsForPlaylist(playlistId: String): Flow<List<Channel>>
    fun getChannelsForUser(userId: String): Flow<List<Channel>>
    fun getFavoriteChannels(): Flow<List<Channel>>
    fun getFavoriteChannels(userId: String): Flow<List<Channel>>
    fun getRecentChannels(): Flow<List<Channel>>
    fun getRecentChannels(userId: String): Flow<List<Channel>>
    fun getDistinctCategories(userId: String, playlistId: String? = null): Flow<List<String>>
    fun getFilteredChannels(
        userId: String,
        playlistId: String?,
        category: String?,
        query: String,
        sort: ChannelSortOption,
        limit: Int = 300
    ): Flow<List<Channel>>
    fun getFilteredChannelsCount(
        userId: String,
        playlistId: String?,
        category: String?,
        query: String
    ): Flow<Int>
    fun getFavoriteCount(userId: String): Flow<Int>
    suspend fun hasPlaybackHistory(userId: String): Boolean
    suspend fun markChannelPlayed(channelId: String)
    suspend fun getChannelCount(playlistId: String): Int
    suspend fun getTotalUserChannelCount(userId: String): Int
    suspend fun toggleFavorite(channelId: String)
    suspend fun setFavorite(channelId: String, isFavorite: Boolean)
    suspend fun updateChannelStatus(channelId: String, status: StreamStatus, latencyMs: Long?)
    suspend fun searchChannels(query: String): List<Channel>
    fun searchChannelsFlow(userId: String, query: String): Flow<List<Channel>>
}
