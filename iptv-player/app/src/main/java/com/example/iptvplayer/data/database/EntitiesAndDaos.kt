package com.example.iptvplayer.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Local User profile entity.
 * Supports username, optional display name, hashed PIN/password with unique salt,
 * custom avatar color/icon, creation timestamp, last login, and active status.
 */
@Entity(
    tableName = "users",
    indices = [Index(value = ["username"], unique = true)]
)
data class UserEntity(
    @PrimaryKey val id: String,
    val username: String,
    val displayName: String?,
    val passwordHash: String?,
    val salt: String?,
    val avatarColor: Long,
    val avatarIcon: String = "person",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val isActive: Boolean = true
)

/**
 * Room Entity definition for playlists.
 * Associated with userId to isolate each user's imported and custom playlists.
 */
@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["userId", "orderIndex"])
    ]
)
data class PlaylistEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val name: String,
    val type: String, // "LOCAL" or "URL"
    val source: String, // file path / URI / URL
    val url: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastSync: Long? = null,
    val channelCount: Int = 0,
    val orderIndex: Int = 0,
    val isCustom: Boolean = false
)

/**
 * Room Entity definition for channels.
 * Associated with userId and playlistId to isolate each user's channels, favorites, and history.
 * Indexed on name, groupTitle, tvgId, tvgName for fast TV projector search and filtering.
 */
@Entity(
    tableName = "channels",
    indices = [
        Index(value = ["userId"]),
        Index(value = ["playlistId"]),
        Index(value = ["userId", "playlistId"]),
        Index(value = ["userId", "isFavorite"]),
        Index(value = ["userId", "lastPlayedAt"]),
        Index(value = ["playlistId", "groupTitle"]),
        Index(value = ["name"]),
        Index(value = ["groupTitle"]),
        Index(value = ["tvgId"]),
        Index(value = ["tvgName"])
    ]
)
data class ChannelEntity(
    @PrimaryKey val id: String,
    val playlistId: String,
    val userId: String = "",
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val groupTitle: String? = null,
    val tvgId: String? = null,
    val tvgName: String? = null,
    val status: String = "UNKNOWN",
    val latency: Long? = null,
    val lastChecked: Long? = null,
    val orderIndex: Int = 0,
    val isFavorite: Boolean = false,
    val lastPlayedAt: Long? = null,
    val playCount: Int = 0
)

/**
 * Cross-reference table for future custom playlists.
 * Allows creating smaller custom playlists referencing existing channels without duplicating data.
 */
@Entity(
    tableName = "playlist_channel_cross_ref",
    primaryKeys = ["playlistId", "channelId"],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["channelId"])
    ]
)
data class PlaylistChannelCrossRef(
    val playlistId: String,
    val channelId: String,
    val customOrderIndex: Int = 0
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE isActive = 1 ORDER BY createdAt ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE username = :username COLLATE NOCASE LIMIT 1")
    suspend fun getUserByUsername(username: String): UserEntity?

    @Query("SELECT COUNT(*) FROM users WHERE isActive = 1")
    suspend fun getUserCount(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    @Query("DELETE FROM users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)
}

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists WHERE userId = :userId ORDER BY orderIndex ASC, createdAt DESC")
    fun getAllPlaylists(userId: String): Flow<List<PlaylistEntity>>

    @Query("SELECT * FROM playlists WHERE id = :id LIMIT 1")
    suspend fun getPlaylistById(id: String): PlaylistEntity?

    @Query("SELECT COUNT(*) FROM playlists WHERE userId = :userId")
    suspend fun getPlaylistCount(userId: String): Int

    @Query("SELECT MAX(orderIndex) FROM playlists WHERE userId = :userId")
    suspend fun getMaxOrderIndex(userId: String): Int?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(playlist: PlaylistEntity)

    @Update
    suspend fun update(playlist: PlaylistEntity)

    @Delete
    suspend fun delete(playlist: PlaylistEntity)

    @Query("DELETE FROM playlists WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE playlists SET orderIndex = :newOrder, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateOrderIndex(id: String, newOrder: Int, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET name = :newName, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateName(id: String, newName: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE playlists SET channelCount = :count, lastSync = :syncTime, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateSyncStats(id: String, count: Int, syncTime: Long, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM playlists WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)
}

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    fun getChannelsForPlaylist(playlistId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE userId = :userId ORDER BY orderIndex ASC")
    fun getChannelsForUser(userId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE userId = :userId AND isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteChannels(userId: String): Flow<List<ChannelEntity>>

    @Query("SELECT * FROM channels WHERE userId = :userId AND lastPlayedAt IS NOT NULL ORDER BY lastPlayedAt DESC LIMIT 20")
    fun getRecentChannels(userId: String): Flow<List<ChannelEntity>>

    @Query("UPDATE channels SET lastPlayedAt = :timestamp WHERE id = :channelId")
    suspend fun updateLastPlayed(channelId: String, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId")
    suspend fun getChannelCountForPlaylist(playlistId: String): Int

    @Query("SELECT COUNT(*) FROM channels WHERE userId = :userId")
    suspend fun getChannelCountForUser(userId: String): Int

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE playlistId = :playlistId AND groupTitle IS NOT NULL AND TRIM(groupTitle) != '' ORDER BY groupTitle COLLATE NOCASE ASC")
    fun getDistinctCategoriesForPlaylist(playlistId: String): Flow<List<String>>

    @Query("SELECT DISTINCT groupTitle FROM channels WHERE userId = :userId AND groupTitle IS NOT NULL AND TRIM(groupTitle) != '' ORDER BY groupTitle COLLATE NOCASE ASC")
    fun getDistinctCategoriesForUser(userId: String): Flow<List<String>>

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId AND groupTitle = :groupTitle")
    suspend fun getChannelCountForGroup(playlistId: String, groupTitle: String): Int

    @Query("SELECT COUNT(*) FROM channels WHERE userId = :userId AND isFavorite = 1")
    fun getFavoriteCountForUser(userId: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM channels WHERE userId = :userId AND lastPlayedAt IS NOT NULL")
    suspend fun getPlaybackHistoryCount(userId: String): Int

    @Query("UPDATE channels SET isFavorite = NOT isFavorite WHERE id = :channelId")
    suspend fun toggleFavorite(channelId: String)

    @Query("UPDATE channels SET isFavorite = :isFavorite WHERE id = :channelId")
    suspend fun setFavorite(channelId: String, isFavorite: Boolean)

    @Query("UPDATE channels SET playCount = playCount + 1, lastPlayedAt = :timestamp WHERE id = :channelId")
    suspend fun incrementPlayCount(channelId: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE channels SET status = :status, latency = :latency, lastChecked = :timestamp WHERE id = :channelId")
    suspend fun updateChannelStatus(channelId: String, status: String, latency: Long?, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM channels WHERE playlistId = :playlistId ORDER BY orderIndex ASC")
    suspend fun getChannelsByPlaylistId(playlistId: String): List<ChannelEntity>

    @Query("UPDATE channels SET status = :status WHERE playlistId = :playlistId")
    suspend fun resetChannelStatusesForPlaylist(playlistId: String, status: String)

    @Query("SELECT COUNT(*) FROM channels WHERE playlistId = :playlistId AND status = :status")
    fun getChannelCountByStatusForPlaylist(playlistId: String, status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM channels WHERE userId = :userId AND status = :status")
    fun getChannelCountByStatusForUser(userId: String, status: String): Flow<Int>

    @Query("SELECT COUNT(*) FROM channels WHERE userId = :userId AND (status = 'ONLINE' OR status = 'OFFLINE')")
    fun getVerifiedChannelCountForUser(userId: String): Flow<Int>

    @Query("""
        SELECT * FROM channels 
        WHERE userId = :userId
          AND playlistId = :playlistId
          AND (:statusFilter IS NULL OR status = :statusFilter)
        ORDER BY
          CASE WHEN :sort = 'NAME_ASC' THEN LOWER(name) END ASC,
          CASE WHEN :sort = 'NAME_DESC' THEN LOWER(name) END DESC,
          CASE WHEN :sort = 'STATUS' THEN 
            CASE status 
              WHEN 'ONLINE' THEN 1 
              WHEN 'CHECKING' THEN 2 
              WHEN 'UNKNOWN' THEN 3 
              WHEN 'OFFLINE' THEN 4 
              ELSE 5 
            END 
          END ASC,
          CASE WHEN :sort = 'LATENCY' THEN (CASE WHEN latency IS NULL THEN 9999999 ELSE latency END) END ASC,
          CASE WHEN :sort = 'LAST_CHECKED' THEN lastChecked END DESC,
          orderIndex ASC
        LIMIT :limit
    """)
    fun getCheckerChannels(
        userId: String,
        playlistId: String,
        statusFilter: String?,
        sort: String,
        limit: Int
    ): Flow<List<ChannelEntity>>

    @Query("""
        SELECT COUNT(*) FROM channels 
        WHERE userId = :userId
          AND playlistId = :playlistId
          AND (:statusFilter IS NULL OR status = :statusFilter)
    """)
    fun getCheckerChannelsCount(
        userId: String,
        playlistId: String,
        statusFilter: String?
    ): Flow<Int>

    @Query("""
        SELECT * FROM channels 
        WHERE userId = :userId
          AND (:playlistId IS NULL OR playlistId = :playlistId)
          AND (:groupTitle IS NULL OR groupTitle = :groupTitle)
          AND (
            :query = '' 
            OR name LIKE '%' || :query || '%' 
            OR tvgName LIKE '%' || :query || '%' 
            OR tvgId LIKE '%' || :query || '%' 
            OR groupTitle LIKE '%' || :query || '%'
          )
        ORDER BY
          CASE WHEN :sort = 'NAME_ASC' THEN LOWER(name) END ASC,
          CASE WHEN :sort = 'NAME_DESC' THEN LOWER(name) END DESC,
          CASE WHEN :sort = 'CATEGORY' THEN LOWER(groupTitle) END ASC,
          CASE WHEN :sort = 'RECENT' THEN lastPlayedAt END DESC,
          CASE WHEN :sort = 'MOST_PLAYED' THEN playCount END DESC,
          orderIndex ASC
        LIMIT :limit
    """)
    fun getFilteredChannels(
        userId: String,
        playlistId: String?,
        groupTitle: String?,
        query: String,
        sort: String,
        limit: Int
    ): Flow<List<ChannelEntity>>

    @Query("""
        SELECT COUNT(*) FROM channels 
        WHERE userId = :userId
          AND (:playlistId IS NULL OR playlistId = :playlistId)
          AND (:groupTitle IS NULL OR groupTitle = :groupTitle)
          AND (
            :query = '' 
            OR name LIKE '%' || :query || '%' 
            OR tvgName LIKE '%' || :query || '%' 
            OR tvgId LIKE '%' || :query || '%' 
            OR groupTitle LIKE '%' || :query || '%'
          )
    """)
    fun getFilteredChannelsCount(
        userId: String,
        playlistId: String?,
        groupTitle: String?,
        query: String
    ): Flow<Int>

    @Query("SELECT * FROM channels WHERE id = :id LIMIT 1")
    suspend fun getChannelById(id: String): ChannelEntity?

    @Query("SELECT * FROM channels WHERE userId = :userId AND (name LIKE '%' || :query || '%' OR groupTitle LIKE '%' || :query || '%' OR tvgName LIKE '%' || :query || '%') ORDER BY name ASC")
    fun searchChannels(userId: String, query: String): Flow<List<ChannelEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<ChannelEntity>)

    @Update
    suspend fun updateChannel(channel: ChannelEntity)

    @Delete
    suspend fun deleteChannel(channel: ChannelEntity)

    @Query("DELETE FROM channels WHERE playlistId = :playlistId")
    suspend fun deleteChannelsByPlaylistId(playlistId: String)

    @Query("DELETE FROM channels WHERE userId = :userId")
    suspend fun deleteAllForUser(userId: String)

    // Cross reference queries for future custom playlists
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCrossRef(crossRef: PlaylistChannelCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllCrossRefs(crossRefs: List<PlaylistChannelCrossRef>)

    @Query("DELETE FROM playlist_channel_cross_ref WHERE playlistId = :playlistId")
    suspend fun deleteCrossRefsForPlaylist(playlistId: String)

    @Query("SELECT c.* FROM channels c INNER JOIN playlist_channel_cross_ref ref ON c.id = ref.channelId WHERE ref.playlistId = :playlistId ORDER BY ref.customOrderIndex ASC")
    fun getChannelsForCustomPlaylist(playlistId: String): Flow<List<ChannelEntity>>
}

@Database(
    entities = [
        UserEntity::class,
        ChannelEntity::class,
        PlaylistEntity::class,
        PlaylistChannelCrossRef::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun channelDao(): ChannelDao
    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "iptv_player_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
