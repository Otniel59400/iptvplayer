package com.example

import com.example.iptvplayer.data.checker.StreamVerifier
import com.example.iptvplayer.domain.model.StreamStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamVerifierTest {

    private val verifier = StreamVerifier()

    @Test
    fun testInvalidUrlsReturnOfflineGracefully() = runBlocking {
        // Blank URL
        val blankResult = verifier.verify("   ")
        assertEquals(StreamStatus.OFFLINE, blankResult.status)
        assertNotNull(blankResult.details)

        // Non HTTP/HTTPS scheme
        val ftpResult = verifier.verify("ftp://example.com/stream.m3u8")
        assertEquals(StreamStatus.OFFLINE, ftpResult.status)

        // Completely malformed URL
        val malformedResult = verifier.verify("http:///:::")
        assertEquals(StreamStatus.OFFLINE, malformedResult.status)
    }

    @Test
    fun testNonExistentHostReturnsOfflineWithoutCrashing() = runBlocking {
        // Unknown host (DNS error)
        val result = verifier.verify("http://nonexistent-domain-for-unit-test-12345.invalid/live.m3u8")
        assertEquals(StreamStatus.OFFLINE, result.status)
        assertTrue(result.details?.contains("DNS", ignoreCase = true) == true || result.details?.isNotEmpty() == true)
    }
}
