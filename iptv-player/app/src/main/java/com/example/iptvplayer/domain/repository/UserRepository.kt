package com.example.iptvplayer.domain.repository

import com.example.iptvplayer.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    val activeUser: StateFlow<User?>

    fun getAllUsers(): Flow<List<User>>
    suspend fun getUserById(id: String): User?
    suspend fun login(userId: String, pinOrPassword: String?): Result<User>
    fun logout()
    suspend fun createUser(
        username: String,
        displayName: String? = null,
        pinOrPassword: String? = null,
        avatarColor: Long = 0xFF00E5FF,
        avatarIcon: String = "person"
    ): Result<User>
    suspend fun updateUser(
        userId: String,
        displayName: String?,
        newPinOrPassword: String?,
        avatarColor: Long? = null
    ): Result<Unit>
    suspend fun deleteUser(userId: String): Result<Unit>
    suspend fun getUserCount(): Int
}
