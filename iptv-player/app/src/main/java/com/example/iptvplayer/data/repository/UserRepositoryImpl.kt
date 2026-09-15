package com.example.iptvplayer.data.repository

import android.content.Context
import com.example.iptvplayer.data.database.UserDao
import com.example.iptvplayer.data.database.UserEntity
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.domain.repository.UserRepository
import com.example.iptvplayer.util.SecurityUtils
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

class UserRepositoryImpl(
    private val userDao: UserDao,
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : UserRepository {

    private val prefs = context.getSharedPreferences("iptv_user_prefs", Context.MODE_PRIVATE)
    private val _activeUser = MutableStateFlow<User?>(null)
    override val activeUser: StateFlow<User?> = _activeUser.asStateFlow()

    init {
        // Restore active user session if exists
        CoroutineScope(ioDispatcher).launch {
            val savedUserId = prefs.getString("current_active_user_id", null)
            if (!savedUserId.isNullOrBlank()) {
                val entity = userDao.getUserById(savedUserId)
                if (entity != null && entity.isActive) {
                    _activeUser.value = entity.toDomain()
                }
            }
        }
    }

    override fun getAllUsers(): Flow<List<User>> {
        return userDao.getAllUsers().map { list ->
            list.map { it.toDomain() }
        }
    }

    override suspend fun getUserById(id: String): User? = withContext(ioDispatcher) {
        userDao.getUserById(id)?.toDomain()
    }

    override suspend fun getUserCount(): Int = withContext(ioDispatcher) {
        userDao.getUserCount()
    }

    override suspend fun login(userId: String, pinOrPassword: String?): Result<User> = withContext(ioDispatcher) {
        val entity = userDao.getUserById(userId)
            ?: return@withContext Result.failure(IllegalArgumentException("Utilizador não encontrado"))

        if (!entity.isActive) {
            return@withContext Result.failure(IllegalStateException("Utilizador inativo"))
        }

        // Validate PIN/password if protected
        if (!entity.passwordHash.isNullOrBlank() && !entity.salt.isNullOrBlank()) {
            val entered = pinOrPassword ?: ""
            val isValid = SecurityUtils.verifyPassword(entered, entity.salt, entity.passwordHash)
            if (!isValid) {
                return@withContext Result.failure(SecurityException("PIN incorreto"))
            }
        }

        // Update last login
        val updated = entity.copy(lastLoginAt = System.currentTimeMillis())
        userDao.updateUser(updated)
        val domainUser = updated.toDomain()

        prefs.edit().putString("current_active_user_id", domainUser.id).apply()
        _activeUser.value = domainUser

        Result.success(domainUser)
    }

    override fun logout() {
        prefs.edit().remove("current_active_user_id").apply()
        _activeUser.value = null
    }

    override suspend fun createUser(
        username: String,
        displayName: String?,
        pinOrPassword: String?,
        avatarColor: Long,
        avatarIcon: String
    ): Result<User> = withContext(ioDispatcher) {
        val cleanUsername = username.trim()
        if (cleanUsername.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("O nome não pode estar vazio"))
        }

        val existing = userDao.getUserByUsername(cleanUsername)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("Já existe um utilizador com este nome"))
        }

        var salt: String? = null
        var hash: String? = null
        if (!pinOrPassword.isNullOrBlank()) {
            val trimmedPin = pinOrPassword.trim()
            if (trimmedPin.length < 4 || trimmedPin.length > 8) {
                return@withContext Result.failure(IllegalArgumentException("PIN inválido. Introduza entre 4 e 8 dígitos"))
            }
            salt = SecurityUtils.generateSalt()
            hash = SecurityUtils.hashPassword(trimmedPin, salt)
        }

        val newEntity = UserEntity(
            id = UUID.randomUUID().toString(),
            username = cleanUsername,
            displayName = displayName?.trim()?.ifBlank { null },
            passwordHash = hash,
            salt = salt,
            avatarColor = avatarColor,
            avatarIcon = avatarIcon,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = System.currentTimeMillis(),
            isActive = true
        )

        userDao.insertUser(newEntity)
        val domainUser = newEntity.toDomain()

        // Set as active session
        prefs.edit().putString("current_active_user_id", domainUser.id).apply()
        _activeUser.value = domainUser

        Result.success(domainUser)
    }

    override suspend fun updateUser(
        userId: String,
        displayName: String?,
        newPinOrPassword: String?,
        avatarColor: Long?
    ): Result<Unit> = withContext(ioDispatcher) {
        val existing = userDao.getUserById(userId)
            ?: return@withContext Result.failure(IllegalArgumentException("Utilizador não encontrado"))

        var salt = existing.salt
        var hash = existing.passwordHash

        if (newPinOrPassword != null) {
            val cleanPin = newPinOrPassword.trim()
            if (cleanPin.isEmpty()) {
                // Disable PIN
                salt = null
                hash = null
            } else {
                if (cleanPin.length < 4 || cleanPin.length > 8) {
                    return@withContext Result.failure(IllegalArgumentException("PIN inválido. Introduza entre 4 e 8 dígitos"))
                }
                salt = SecurityUtils.generateSalt()
                hash = SecurityUtils.hashPassword(cleanPin, salt)
            }
        }

        val updated = existing.copy(
            displayName = displayName?.trim()?.ifBlank { null } ?: existing.displayName,
            avatarColor = avatarColor ?: existing.avatarColor,
            passwordHash = hash,
            salt = salt
        )
        userDao.updateUser(updated)

        if (_activeUser.value?.id == userId) {
            _activeUser.value = updated.toDomain()
        }

        Result.success(Unit)
    }

    override suspend fun deleteUser(userId: String): Result<Unit> = withContext(ioDispatcher) {
        val count = userDao.getUserCount()
        if (count <= 1) {
            return@withContext Result.failure(IllegalStateException("Não é possível eliminar o único utilizador"))
        }

        userDao.deleteUserById(userId)

        if (_activeUser.value?.id == userId) {
            logout()
        }

        Result.success(Unit)
    }

    private fun UserEntity.toDomain(): User {
        return User(
            id = id,
            username = username,
            displayName = displayName,
            isPinProtected = !passwordHash.isNullOrBlank(),
            avatarColor = avatarColor,
            avatarIcon = avatarIcon,
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            isActive = isActive
        )
    }
}
