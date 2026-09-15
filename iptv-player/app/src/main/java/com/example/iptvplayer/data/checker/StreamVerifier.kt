package com.example.iptvplayer.data.checker

import com.example.iptvplayer.domain.model.StreamStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ConnectionPool
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLHandshakeException

/**
 * Result of checking an IPTV stream endpoint.
 */
data class StreamVerificationResult(
    val status: StreamStatus,
    val latencyMs: Long?,
    val details: String? = null
)

/**
 * Lightweight HTTP/HLS Stream Verifier optimized for Android TV / HY300 projector.
 * Features:
 * - 4-second strict timeouts
 * - Low bandwidth consumption (does not download whole streams, inspects manifests and headers)
 * - Verifies HLS Master and Media playlists, media segments, and MPEG-TS headers
 * - Accurate network check latency measurement
 */
class StreamVerifier(
    private val client: OkHttpClient = createDefaultOkHttpClient()
) {
    companion object {
        private const val USER_AGENT = "IPTVPlayer/1.0 (Android; HY300; Linux; Media3)"
        private const val TIMEOUT_SECONDS = 4L
        private const val MAX_MANIFEST_BYTES = 65536L // 64 KB max to save bandwidth

        fun createDefaultOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .callTimeout(TIMEOUT_SECONDS + 2L, TimeUnit.SECONDS)
                .followRedirects(true)
                .followSslRedirects(true)
                .connectionPool(ConnectionPool(5, 30, TimeUnit.SECONDS))
                .build()
        }
    }

    /**
     * Inspects the stream URL and returns reachability/functionality status.
     * Never crashes; handles all network and protocol exceptions.
     */
    suspend fun verify(streamUrl: String): StreamVerificationResult = withContext(Dispatchers.IO) {
        val trimmedUrl = streamUrl.trim()
        if (trimmedUrl.isBlank() || (!trimmedUrl.startsWith("http://", ignoreCase = true) && !trimmedUrl.startsWith("https://", ignoreCase = true))) {
            return@withContext StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = null,
                details = "URL de stream inválido"
            )
        }

        val httpUrl = trimmedUrl.toHttpUrlOrNull()
        if (httpUrl == null) {
            return@withContext StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = null,
                details = "Formato de URL inválido"
            )
        }

        val startTime = System.currentTimeMillis()
        try {
            val isHls = isHlsUrl(trimmedUrl)
            if (isHls) {
                verifyHlsStream(trimmedUrl, startTime)
            } else {
                verifyGenericOrTsStream(trimmedUrl, startTime)
            }
        } catch (e: SocketTimeoutException) {
            val latency = System.currentTimeMillis() - startTime
            StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = latency,
                details = "Tempo limite esgotado (timeout)"
            )
        } catch (e: UnknownHostException) {
            StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = null,
                details = "Servidor não encontrado (falha DNS)"
            )
        } catch (e: ConnectException) {
            StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = null,
                details = "Ligação recusada pelo servidor"
            )
        } catch (e: SSLHandshakeException) {
            StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = null,
                details = "Erro de certificado SSL/TLS"
            )
        } catch (e: IOException) {
            val latency = System.currentTimeMillis() - startTime
            StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = latency,
                details = e.message ?: "Erro de rede"
            )
        } catch (e: Exception) {
            StreamVerificationResult(
                status = StreamStatus.UNKNOWN,
                latencyMs = null,
                details = e.message ?: "Erro inesperado"
            )
        }
    }

    private fun isHlsUrl(url: String): Boolean {
        val lower = url.lowercase()
        return lower.contains(".m3u8") || lower.contains("/hls/") || lower.contains("format=m3u8")
    }

    /**
     * Inspects HLS stream manifest:
     * 1. Request manifest text
     * 2. Validate #EXTM3U
     * 3. If master playlist, resolve first sub-playlist
     * 4. Check for media segments (#EXTINF or .ts/.m4s/.aac)
     * 5. Check first segment reachability (HEAD or Range)
     */
    private fun verifyHlsStream(url: String, startTime: Long): StreamVerificationResult {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .build()

        client.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                return StreamVerificationResult(
                    status = StreamStatus.OFFLINE,
                    latencyMs = latency,
                    details = "HTTP ${response.code} ${response.message}"
                )
            }

            val body = response.body ?: return StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = latency,
                details = "Resposta vazia do servidor"
            )

            // Read up to 64KB
            val source = body.source()
            source.request(MAX_MANIFEST_BYTES)
            val buffer = source.buffer
            val maxBytes = minOf(buffer.size, MAX_MANIFEST_BYTES)
            val manifestText = buffer.clone().readUtf8(maxBytes)

            val lower = manifestText.lowercase()
            // Detect web server error / cloudflare HTML pages returning HTTP 200
            if (lower.contains("<!doctype html") || lower.contains("<html") || lower.contains("cloudflare") || lower.contains("access denied")) {
                return StreamVerificationResult(
                    status = StreamStatus.OFFLINE,
                    latencyMs = latency,
                    details = "Página HTML / Acesso negado"
                )
            }

            if (!manifestText.contains("#EXTM3U")) {
                return StreamVerificationResult(
                    status = StreamStatus.UNKNOWN,
                    latencyMs = latency,
                    details = "Conteúdo não contém cabeçalho #EXTM3U"
                )
            }

            // Check if Master / Multivariant Playlist
            if (manifestText.contains("#EXT-X-STREAM-INF")) {
                val subPlaylistUri = extractSubPlaylistUri(manifestText)
                if (!subPlaylistUri.isNullOrBlank()) {
                    val resolvedUrl = resolveRelativeUrl(url, subPlaylistUri)
                    if (resolvedUrl != null) {
                        return verifyHlsMediaPlaylist(resolvedUrl, startTime)
                    }
                }
                return StreamVerificationResult(
                    status = StreamStatus.ONLINE,
                    latencyMs = latency,
                    details = "Master playlist HLS válida"
                )
            }

            // Check if Media Playlist contains segments
            val hasSegments = manifestText.contains("#EXTINF") ||
                    manifestText.lines().any { line ->
                        val t = line.trim()
                        t.isNotEmpty() && !t.startsWith("#") && (t.contains(".ts") || t.contains(".m4s") || t.contains(".aac") || t.contains("segment") || t.contains(".mp4"))
                    }

            if (hasSegments) {
                val firstSegment = extractFirstSegmentUri(manifestText)
                if (firstSegment != null) {
                    val resolvedSeg = resolveRelativeUrl(url, firstSegment)
                    if (resolvedSeg != null) {
                        val segCheck = testSegmentReachability(resolvedSeg)
                        if (segCheck == false) {
                            return StreamVerificationResult(
                                status = StreamStatus.OFFLINE,
                                latencyMs = latency,
                                details = "Segmentos de vídeo inacessíveis"
                            )
                        }
                    }
                }
                return StreamVerificationResult(
                    status = StreamStatus.ONLINE,
                    latencyMs = latency,
                    details = "Stream HLS ativo"
                )
            } else {
                return StreamVerificationResult(
                    status = StreamStatus.UNKNOWN,
                    latencyMs = latency,
                    details = "Manifesto HLS sem segmentos identificáveis"
                )
            }
        }
    }

    private fun verifyHlsMediaPlaylist(mediaPlaylistUrl: String, startTime: Long): StreamVerificationResult {
        val request = Request.Builder()
            .url(mediaPlaylistUrl)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .build()

        client.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime
            if (!response.isSuccessful) {
                return StreamVerificationResult(
                    status = StreamStatus.OFFLINE,
                    latencyMs = latency,
                    details = "Sub-playlist HTTP ${response.code}"
                )
            }

            val body = response.body ?: return StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = latency,
                details = "Sub-playlist vazia"
            )

            val source = body.source()
            source.request(MAX_MANIFEST_BYTES)
            val buffer = source.buffer
            val text = buffer.clone().readUtf8(minOf(buffer.size, MAX_MANIFEST_BYTES))

            val hasSegments = text.contains("#EXTINF") ||
                    text.lines().any { line ->
                        val t = line.trim()
                        t.isNotEmpty() && !t.startsWith("#")
                    }

            return if (hasSegments) {
                StreamVerificationResult(
                    status = StreamStatus.ONLINE,
                    latencyMs = latency,
                    details = "Sub-playlist HLS ativa"
                )
            } else {
                StreamVerificationResult(
                    status = StreamStatus.UNKNOWN,
                    latencyMs = latency,
                    details = "Sub-playlist sem segmentos"
                )
            }
        }
    }

    /**
     * Checks generic streams (MPEG-TS, MP4, live streams).
     * Uses Range: bytes=0-2048 to avoid downloading whole video.
     */
    private fun verifyGenericOrTsStream(url: String, startTime: Long): StreamVerificationResult {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Range", "bytes=0-2048")
            .header("Accept", "*/*")
            .build()

        client.newCall(request).execute().use { response ->
            val latency = System.currentTimeMillis() - startTime

            if (!response.isSuccessful && response.code != 206) {
                return StreamVerificationResult(
                    status = StreamStatus.OFFLINE,
                    latencyMs = latency,
                    details = "HTTP ${response.code} ${response.message}"
                )
            }

            val contentType = response.header("Content-Type")?.lowercase() ?: ""
            if (contentType.contains("text/html")) {
                return StreamVerificationResult(
                    status = StreamStatus.OFFLINE,
                    latencyMs = latency,
                    details = "Retornou página HTML"
                )
            }

            val body = response.body ?: return StreamVerificationResult(
                status = StreamStatus.OFFLINE,
                latencyMs = latency,
                details = "Sem corpo de resposta"
            )

            val source = body.source()
            source.request(512)
            val buffer = source.buffer
            if (buffer.size == 0L) {
                return StreamVerificationResult(
                    status = StreamStatus.OFFLINE,
                    latencyMs = latency,
                    details = "Stream sem dados"
                )
            }

            // Check if it's an HLS playlist disguised without .m3u8 extension
            val prefix = buffer.clone().readUtf8(minOf(buffer.size, 128L))
            if (prefix.contains("#EXTM3U")) {
                return verifyHlsStream(url, startTime)
            }

            // Check for MPEG-TS sync byte 0x47 (71)
            val firstByte = buffer[0]
            val isMpegTs = (firstByte == 0x47.toByte()) || contentType.contains("video/mp2t")
            val isMedia = contentType.startsWith("video/") || contentType.startsWith("audio/") || isMpegTs

            if (isMedia) {
                return StreamVerificationResult(
                    status = StreamStatus.ONLINE,
                    latencyMs = latency,
                    details = "Stream multimédia verificado"
                )
            }

            // If 200/206 returned with unknown binary format, classify as UNKNOWN rather than claiming Online
            return StreamVerificationResult(
                status = StreamStatus.UNKNOWN,
                latencyMs = latency,
                details = "Protocolo ou formato indeterminado"
            )
        }
    }

    private fun testSegmentReachability(segmentUrl: String): Boolean? {
        return try {
            val request = Request.Builder()
                .url(segmentUrl)
                .header("User-Agent", USER_AGENT)
                .header("Range", "bytes=0-1024")
                .build()

            client.newCall(request).execute().use { res ->
                if (res.code in 200..299 || res.code == 206) true
                else if (res.code in 400..599) false
                else null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractSubPlaylistUri(manifestText: String): String? {
        var foundStreamInf = false
        for (line in manifestText.lines()) {
            val trimmed = line.trim()
            if (foundStreamInf && trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                return trimmed
            }
            if (trimmed.startsWith("#EXT-X-STREAM-INF")) {
                foundStreamInf = true
            }
        }
        return null
    }

    private fun extractFirstSegmentUri(manifestText: String): String? {
        for (line in manifestText.lines()) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                return trimmed
            }
        }
        return null
    }

    private fun resolveRelativeUrl(baseUrl: String, relativeUri: String): String? {
        val trimmed = relativeUri.trim()
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            return trimmed
        }
        val base = baseUrl.toHttpUrlOrNull() ?: return null
        return base.resolve(trimmed)?.toString()
    }
}
