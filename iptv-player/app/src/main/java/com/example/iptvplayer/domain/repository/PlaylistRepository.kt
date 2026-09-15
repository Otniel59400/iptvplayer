package com.example.iptvplayer.domain.repository

import com.example.iptvplayer.domain.model.Playlist
import kotlinx.coroutines.flow.Flow
import java.io.InputStream

interface PlaylistRepository {
    fun getPlaylists(userId: String): Flow<List<Playlist>>
    fun getPlaylists(): Flow<List<Playlist>>
    suspend fun getPlaylistById(id: String): Playlist?

    suspend fun importLocalPlaylist(
        userId: String,
        name: String,
        inputStream: InputStream,
        sourceDescription: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Playlist>

    suspend fun importRemotePlaylist(
        userId: String,
        name: String,
        url: String,
        onProgress: ((Int) -> Unit)? = null
    ): Result<Playlist>

    suspend fun renamePlaylist(id: String, newName: String): Result<Unit>
    suspend fun deletePlaylist(id: String): Result<Unit>
    suspend fun duplicatePlaylist(id: String): Result<Playlist>
    suspend fun reorderPlaylist(playlistId: String, moveUp: Boolean, userId: String): Result<Unit>
    suspend fun refreshRemotePlaylist(id: String, onProgress: ((Int) -> Unit)? = null): Result<Playlist>

    // Compatibility & Custom Playlists
    suspend fun addPlaylist(name: String, sourceUrl: String?, isLocal: Boolean): Playlist
    suspend fun createCustomPlaylist(name: String, selectedChannelIds: List<String>): Playlist
    suspend fun exportPlaylist(id: String): String
}
