package com.example.iptvplayer.util

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Offline secure hashing utility for PINs and passwords.
 * Never stores plain text passwords or PINs.
 * Uses PBKDF2WithHmacSHA256 with cryptographically strong salt and constant-time verification.
 */
object SecurityUtils {

    private const val ITERATIONS = 10_000
    private const val KEY_LENGTH = 256
    private val secureRandom = SecureRandom()

    fun generateSalt(byteLength: Int = 16): String {
        val salt = ByteArray(byteLength)
        secureRandom.nextBytes(salt)
        return salt.joinToString("") { "%02x".format(it) }
    }

    fun hashPassword(password: String, saltHex: String): String {
        return try {
            val saltBytes = hexToByteArray(saltHex)
            val spec = PBEKeySpec(password.toCharArray(), saltBytes, ITERATIONS, KEY_LENGTH)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val hash = factory.generateSecret(spec).encoded
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Fallback to SHA-256 with salt
            fallbackSha256(password, saltHex)
        }
    }

    fun verifyPassword(password: String, saltHex: String, storedHashHex: String): Boolean {
        if (password.isEmpty() && storedHashHex.isEmpty()) return true
        val computedHash = hashPassword(password, saltHex)
        return MessageDigest.isEqual(computedHash.toByteArray(), storedHashHex.toByteArray())
    }

    private fun fallbackSha256(password: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(hexToByteArray(saltHex))
        var hash = digest.digest(password.toByteArray(Charsets.UTF_8))
        for (i in 0 until 5000) {
            digest.reset()
            hash = digest.digest(hash)
        }
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun hexToByteArray(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) +
                    Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
