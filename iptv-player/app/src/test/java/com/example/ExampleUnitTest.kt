package com.example

import com.example.iptvplayer.util.SecurityUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityAndUserUnitTest {

    @Test
    fun testSaltGenerationIsUnique() {
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()
        assertNotEquals(salt1, salt2)
        assertEquals(32, salt1.length) // 16 bytes = 32 hex chars
    }

    @Test
    fun testPasswordHashingAndVerification() {
        val pin = "1234"
        val salt = SecurityUtils.generateSalt()
        val hash = SecurityUtils.hashPassword(pin, salt)

        // Verifying correct PIN should succeed
        assertTrue(SecurityUtils.verifyPassword("1234", salt, hash))

        // Verifying incorrect PIN should fail
        assertFalse(SecurityUtils.verifyPassword("9999", salt, hash))
        assertFalse(SecurityUtils.verifyPassword("12345", salt, hash))
        assertFalse(SecurityUtils.verifyPassword("", salt, hash))
    }

    @Test
    fun testDifferentSaltsProduceDifferentHashes() {
        val pin = "2026"
        val salt1 = SecurityUtils.generateSalt()
        val salt2 = SecurityUtils.generateSalt()

        val hash1 = SecurityUtils.hashPassword(pin, salt1)
        val hash2 = SecurityUtils.hashPassword(pin, salt2)

        assertNotEquals(hash1, hash2)
    }
}
