package com.example.iptvplayer.data.repository

import com.example.iptvplayer.data.database.AppDatabase
import com.example.iptvplayer.data.database.ChannelEntity
import com.example.iptvplayer.data.database.PlaylistEntity
import com.example.iptvplayer.data.parser.M3UParser
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.Playlist
import com.example.iptvplayer.domain.model.PlaylistType
import com.example.iptvplayer.domain.repository.PlaylistRepository
import com.example.iptvplayer.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.util.UUID

class PlaylistRepositoryImpl(
    private val database: AppDatabase,
    private val m3uParser: M3UParser,
    private val userRepository: UserRepository
) : PlaylistRepository {

    private val playlistDao = database.playlistDao()
    private val channelDao = database.channelDao()

    override fun getPlaylists(userId: String): Flow<List<Playlist>> {
        return playlistDao.getAllPlaylists(userId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(Dispatchers.IO)
    }

    override fun getPlaylists(): Flow<List<Playlist>> {
        val currentUserId = userRepository.activeUser.value?.id ?: ""
        return getPlaylists(currentUserId)
    }

    override suspend fun getPlaylistById(id: String): Playlist? = withContext(Dispatchers.IO) {
        playlistDao.getPlaylistById(id)?.toDomain()
    }

    override suspend fun importLocalPlaylist(
        userId: String,
        name: String,
        inputStream: InputStream,
        sourceDescription: String,
        onProgress: ((Int) -> Unit)?
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = UUID.randomUUID().toString()
            val maxOrder = playlistDao.getMaxOrderIndex(userId) ?: -1
            val nextOrder = maxOrder + 1

            val channels = m3uParser.parse(
                inputStream = inputStream,
                playlistId = playlistId,
                userId = userId,
                onProgress = onProgress
            )

            if (channels.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("O ficheiro M3U não contém nenhum canal válido.")
                )
            }

            val now = System.currentTimeMillis()
            val entity = PlaylistEntity(
                id = playlistId,
                userId = userId,
                name = name.trim().ifBlank { "Playlist Local" },
                type = "LOCAL",
                source = sourceDescription,
                url = null,
                channelCount = channels.size,
                orderIndex = nextOrder,
                createdAt = now,
                updatedAt = now,
                lastSync = now,
                isCustom = false
            )

            playlistDao.insert(entity)

            // Insert channels in chunks of 500 to keep RAM low and prevent SQLite transaction limits
            channels.chunked(500).forEach { chunk ->
                val entities = chunk.map { it.toEntity(playlistId, userId) }
                channelDao.insertAll(entities)
            }

            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun importRemotePlaylist(
        userId: String,
        name: String,
        url: String,
        onProgress: ((Int) -> Unit)?
    ): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlistId = UUID.randomUUID().toString()
            val maxOrder = playlistDao.getMaxOrderIndex(userId) ?: -1
            val nextOrder = maxOrder + 1

            val channels = m3uParser.parseUrl(
                url = url.trim(),
                playlistId = playlistId,
                userId = userId,
                onProgress = onProgress
            )

            if (channels.isEmpty()) {
                return@withContext Result.failure(
                    IllegalArgumentException("Nenhum canal foi retornado pelo servidor da playlist.")
                )
            }

            val now = System.currentTimeMillis()
            val entity = PlaylistEntity(
                id = playlistId,
                userId = userId,
                name = name.trim().ifBlank { "Playlist Remota" },
                type = "URL",
                source = url.trim(),
                url = url.trim(),
                channelCount = channels.size,
                orderIndex = nextOrder,
                createdAt = now,
                updatedAt = now,
                lastSync = now,
                isCustom = false
            )

            playlistDao.insert(entity)

            // Insert channels in chunks of 500
            channels.chunked(500).forEach { chunk ->
                val entities = chunk.map { it.toEntity(playlistId, userId) }
                channelDao.insertAll(entities)
            }

            Result.success(entity.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun renamePlaylist(id: String, newName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (newName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("O nome da playlist não pode ser vazio."))
            }
            playlistDao.updateName(id, newName.trim())
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deletePlaylist(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            channelDao.deleteChannelsByPlaylistId(id)
            channelDao.deleteCrossRefsForPlaylist(id)
            playlistDao.deleteById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun duplicatePlaylist(id: String): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val original = playlistDao.getPlaylistById(id)
                ?: return@withContext Result.failure(IllegalArgumentException("Playlist original não encontrada."))

            val newId = UUID.randomUUID().toString()
            val maxOrder = playlistDao.getMaxOrderIndex(original.userId) ?: -1
            val now = System.currentTimeMillis()

            val duplicate = original.copy(
                id = newId,
                name = "${original.name} (Cópia)",
                orderIndex = maxOrder + 1,
                createdAt = now,
                updatedAt = now
            )
            playlistDao.insert(duplicate)

            // Read original channels and duplicate them
            val originalChannels = channelDao.getChannelsForPlaylist(id).firstOrNull() ?: emptyList()
            if (originalChannels.isNotEmpty()) {
                originalChannels.chunked(500).forEach { chunk ->
                    val duplicatedChunk = chunk.map { ch ->
                        ch.copy(
                            id = UUID.randomUUID().toString(),
                            playlistId = newId,
                            isFavorite = false
                        )
                    }
                    channelDao.insertAll(duplicatedChunk)
                }
            }

            Result.success(duplicate.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun reorderPlaylist(playlistId: String, moveUp: Boolean, userId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val playlists = playlistDao.getAllPlaylists(userId).firstOrNull() ?: return@withContext Result.success(Unit)
            val index = playlists.indexOfFirst { it.id == playlistId }
            if (index == -1) return@withContext Result.success(Unit)

            val targetIndex = if (moveUp) index - 1 else index + 1
            if (targetIndex !in playlists.indices) return@withContext Result.success(Unit)

            val current = playlists[index]
            val target = playlists[targetIndex]

            // Swap their orderIndex
            playlistDao.updateOrderIndex(current.id, target.orderIndex)
            playlistDao.updateOrderIndex(target.id, current.orderIndex)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun refreshRemotePlaylist(id: String, onProgress: ((Int) -> Unit)?): Result<Playlist> = withContext(Dispatchers.IO) {
        try {
            val playlist = playlistDao.getPlaylistById(id)
                ?: return@withContext Result.failure(IllegalArgumentException("Playlist não encontrada."))

            val targetUrl = playlist.url ?: playlist.source
            if (playlist.type != "URL" || targetUrl.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("Esta playlist é local e não suporta atualização remota."))
            }

            val channels = m3uParser.parseUrl(
                url = targetUrl,
                playlistId = id,
                userId = playlist.userId,
                onProgress = onProgress
            )

            // Delete old channels and insert new ones in transaction chunks
            channelDao.deleteChannelsByPlaylistId(id)
            channels.chunked(500).forEach { chunk ->
                val entities = chunk.map { it.toEntity(id, playlist.userId) }
                channelDao.insertAll(entities)
            }

            val now = System.currentTimeMillis()
            playlistDao.updateSyncStats(id, channels.size, now)

            val updated = playlist.copy(
                channelCount = channels.size,
                lastSync = now,
                updatedAt = now
            )
            Result.success(updated.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addPlaylist(name: String, sourceUrl: String?, isLocal: Boolean): Playlist {
        val currentUserId = userRepository.activeUser.value?.id ?: ""
        val playlistId = UUID.randomUUID().toString()
        val maxOrder = playlistDao.getMaxOrderIndex(currentUserId) ?: -1
        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(
            id = playlistId,
            userId = currentUserId,
            name = name,
            type = if (isLocal) "LOCAL" else "URL",
            source = sourceUrl ?: "",
            url = if (!isLocal) sourceUrl else null,
            channelCount = 0,
            orderIndex = maxOrder + 1,
            createdAt = now,
            updatedAt = now,
            lastSync = now,
            isCustom = false
        )
        playlistDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun createCustomPlaylist(name: String, selectedChannelIds: List<String>): Playlist {
        val currentUserId = userRepository.activeUser.value?.id ?: ""
        val playlistId = UUID.randomUUID().toString()
        val maxOrder = playlistDao.getMaxOrderIndex(currentUserId) ?: -1
        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(
            id = playlistId,
            userId = currentUserId,
            name = name,
            type = "CUSTOM",
            source = "",
            url = null,
            channelCount = selectedChannelIds.size,
            orderIndex = maxOrder + 1,
            createdAt = now,
            updatedAt = now,
            lastSync = now,
            isCustom = true
        )
        playlistDao.insert(entity)
        return entity.toDomain()
    }

    override suspend fun exportPlaylist(id: String): String {
        return "#EXTM3U\n# Playlist exported by IPTV Player\n"
    }

    private fun PlaylistEntity.toDomain(): Playlist {
        val pType = when (type.uppercase()) {
            "URL" -> PlaylistType.URL
            "CUSTOM" -> PlaylistType.CUSTOM
            else -> PlaylistType.LOCAL
        }
        return Playlist(
            id = id,
            userId = userId,
            name = name,
            type = pType,
            source = source,
            url = url,
            channelCount = channelCount,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastSync = lastSync,
            orderIndex = orderIndex,
            isCustom = isCustom
        )
    }

    private fun Channel.toEntity(playlistId: String, userId: String): ChannelEntity {
        return ChannelEntity(
            id = id,
            playlistId = playlistId,
            userId = userId,
            name = name,
            streamUrl = streamUrl,
            logoUrl = logoUrl,
            groupTitle = groupTitle ?: category,
            tvgId = tvgId,
            tvgName = tvgName,
            status = status.name,
            latency = latencyMs,
            lastChecked = lastCheckedTimestamp,
            orderIndex = orderIndex,
            isFavorite = isFavorite,
            lastPlayedAt = lastPlayedAt
        )
    }
}
