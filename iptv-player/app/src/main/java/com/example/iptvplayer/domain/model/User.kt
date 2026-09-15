package com.example.iptvplayer.domain.model

/**
 * Domain representation of an application user profile.
 */
data class User(
    val id: String,
    val username: String,
    val displayName: String? = null,
    val isPinProtected: Boolean = false,
    val avatarColor: Long = 0xFF00E5FF,
    val avatarIcon: String = "person",
    val createdAt: Long = System.currentTimeMillis(),
    val lastLoginAt: Long? = null,
    val isActive: Boolean = true
) {
    val displayTitle: String
        get() = if (!displayName.isNullOrBlank()) displayName else username
}
