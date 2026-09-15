package com.example

import com.example.iptvplayer.data.parser.M3UParserImpl
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets

class M3UParserTest {

    private val parser = M3UParserImpl()

    @Test
    fun testParseStandardM3U() = runBlocking {
        val m3uContent = """
            #EXTM3U
            #EXTINF:-1 tvg-id="tpa1" tvg-name="TPA 1" tvg-logo="https://example.com/tpa1.png" group-title="Nacional",TPA 1 Angola
            http://stream.example.com/tpa1/live.m3u8
            #EXTINF:-1 tvg-id="zapviva" group-title="Entretenimento",ZAP Viva
            http://stream.example.com/zapviva/index.m3u8
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(StandardCharsets.UTF_8))
        val channels = parser.parse(inputStream, "test-playlist-1", "user-1")

        assertEquals(2, channels.size)

        val ch1 = channels[0]
        assertEquals("TPA 1 Angola", ch1.name)
        assertEquals("http://stream.example.com/tpa1/live.m3u8", ch1.streamUrl)
        assertEquals("https://example.com/tpa1.png", ch1.logoUrl)
        assertEquals("Nacional", ch1.groupTitle)
        assertEquals("tpa1", ch1.tvgId)
        assertEquals("TPA 1", ch1.tvgName)

        val ch2 = channels[1]
        assertEquals("ZAP Viva", ch2.name)
        assertEquals("http://stream.example.com/zapviva/index.m3u8", ch2.streamUrl)
        assertEquals("Entretenimento", ch2.groupTitle)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testParseEmptyOrMalformedM3U(): Unit = runBlocking {
        val m3uContent = """
            #EXTM3U
            # Just comments without valid stream
            # Another comment
        """.trimIndent()

        val inputStream = ByteArrayInputStream(m3uContent.toByteArray(StandardCharsets.UTF_8))
        parser.parse(inputStream, "test-playlist-2", "user-1")
    }
}
