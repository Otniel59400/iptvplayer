package com.example.iptvplayer.data.parser

import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.StreamStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Robust, memory-efficient M3U / M3U8 streaming parser for large playlists (20,000+ channels).
 * Optimized for low RAM / CPU on projector devices such as the HY300.
 */
interface M3UParser {
    suspend fun parse(
        inputStream: InputStream,
        playlistId: String = "",
        userId: String = "",
        onProgress: ((count: Int) -> Unit)? = null
    ): List<Channel>

    suspend fun parseUrl(
        url: String,
        playlistId: String = "",
        userId: String = "",
        onProgress: ((count: Int) -> Unit)? = null
    ): List<Channel>
}

class M3UParserImpl(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .callTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) : M3UParser {

    private val attributePattern = Pattern.compile("""([a-zA-Z0-9_-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([^\s,]+))""")

    override suspend fun parse(
        inputStream: InputStream,
        playlistId: String,
        userId: String,
        onProgress: ((count: Int) -> Unit)?
    ): List<Channel> = withContext(Dispatchers.IO) {
        val channels = mutableListOf<Channel>()
        val seenIdenticalEntries = HashSet<String>()
        var currentTvgId: String? = null
        var currentTvgName: String? = null
        var currentTvgLogo: String? = null
        var currentGroupTitle: String? = null
        var currentChannelName: String? = null
        var hasPendingInf = false
        var order = 0

        BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
            var line: String? = reader.readLine()
            while (line != null) {
                val trimmed = line.trim()
                if (trimmed.isEmpty()) {
                    line = reader.readLine()
                    continue
                }

                if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                    hasPendingInf = true
                    val commaIndex = trimmed.indexOf(',')
                    val attrPart = if (commaIndex != -1) trimmed.substring(0, commaIndex) else trimmed
                    val namePart = if (commaIndex != -1) trimmed.substring(commaIndex + 1).trim() else ""

                    currentTvgId = extractAttribute(attrPart, "tvg-id")
                    currentTvgName = extractAttribute(attrPart, "tvg-name")
                    currentTvgLogo = extractAttribute(attrPart, "tvg-logo")
                    currentGroupTitle = extractAttribute(attrPart, "group-title")
                    currentChannelName = namePart.ifBlank { currentTvgName ?: currentTvgId ?: "Canal" }
                } else if (trimmed.startsWith("#EXTGRP:", ignoreCase = true)) {
                    if (currentGroupTitle.isNullOrBlank()) {
                        currentGroupTitle = trimmed.substringAfter(":").trim()
                    }
                } else if (trimmed.startsWith("#")) {
                    // Skip other headers/comments (#EXTM3U, #EXTVLCOPT, etc.)
                } else {
                    // Stream URL line
                    if (hasPendingInf) {
                        val streamUrl = trimmed
                        if (streamUrl.startsWith("http://", ignoreCase = true) ||
                            streamUrl.startsWith("https://", ignoreCase = true) ||
                            streamUrl.startsWith("rtmp://", ignoreCase = true) ||
                            streamUrl.startsWith("rtsp://", ignoreCase = true) ||
                            streamUrl.startsWith("mms://", ignoreCase = true) ||
                            streamUrl.contains("://")
                        ) {
                            val finalChannelName = currentChannelName?.ifBlank { null } ?: "Canal ${order + 1}"
                            val dedupeKey = "${finalChannelName.trim().lowercase()}###${streamUrl.trim()}"
                            if (seenIdenticalEntries.add(dedupeKey)) {
                                val category = currentGroupTitle?.trim()?.ifBlank { null } ?: "Geral"
                                val channel = Channel(
                                    id = UUID.randomUUID().toString(),
                                    playlistId = playlistId,
                                    userId = userId,
                                    name = finalChannelName,
                                    category = category,
                                    groupTitle = category,
                                    streamUrl = streamUrl,
                                    logoUrl = currentTvgLogo?.trim()?.ifBlank { null },
                                    tvgId = currentTvgId?.trim()?.ifBlank { null },
                                    tvgName = currentTvgName?.trim()?.ifBlank { null },
                                    status = StreamStatus.UNKNOWN,
                                    orderIndex = order++
                                )
                                channels.add(channel)

                                if (channels.size % 250 == 0) {
                                    onProgress?.invoke(channels.size)
                                }
                            }
                        }

                        // Reset pending state
                        hasPendingInf = false
                        currentTvgId = null
                        currentTvgName = null
                        currentTvgLogo = null
                        currentGroupTitle = null
                        currentChannelName = null
                    }
                }

                line = reader.readLine()
            }
        }

        onProgress?.invoke(channels.size)
        if (channels.isEmpty()) {
            throw IllegalArgumentException("Nenhum canal válido foi encontrado no conteúdo M3U.")
        }

        channels
    }

    override suspend fun parseUrl(
        url: String,
        playlistId: String,
        userId: String,
        onProgress: ((count: Int) -> Unit)?
    ): List<Channel> = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
            !cleanUrl.startsWith("https://", ignoreCase = true)
        ) {
            throw IllegalArgumentException("URL inválido. Introduza um endereço que comece por http:// ou https://")
        }

        val request = Request.Builder()
            .url(cleanUrl)
            .header("User-Agent", "IPTVPlayer/1.0 (Android; Leanback; HY300)")
            .header("Accept", "*/*")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("Erro do servidor HTTP: ${response.code} ${response.message}")
                }
                val body = response.body
                    ?: throw IOException("Resposta vazia recebida do servidor da playlist.")

                body.byteStream().use { stream ->
                    parse(
                        inputStream = stream,
                        playlistId = playlistId,
                        userId = userId,
                        onProgress = onProgress
                    )
                }
            }
        } catch (e: UnknownHostException) {
            throw IOException("Não foi possível resolver o endereço do servidor. Verifique o URL ou a ligação à internet.", e)
        } catch (e: SocketTimeoutException) {
            throw IOException("Tempo limite excedido ao descarregar a playlist remota.", e)
        } catch (e: ConnectException) {
            throw IOException("Falha ao ligar ao servidor da playlist. Verifique se o endereço está correto e acessível.", e)
        }
    }

    private fun extractAttribute(source: String, key: String): String? {
        val matcher = attributePattern.matcher(source)
        while (matcher.find()) {
            val attrKey = matcher.group(1)
            if (attrKey.equals(key, ignoreCase = true)) {
                return matcher.group(2) ?: matcher.group(3) ?: matcher.group(4)
            }
        }
        return null
    }
}

