package com.example.iptvplayer.data.checker

import com.example.iptvplayer.data.database.AppDatabase
import com.example.iptvplayer.data.database.ChannelEntity
import com.example.iptvplayer.domain.model.StreamStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * Real-time progress snapshot of the channel verification process.
 */
data class CheckerProgressState(
    val isRunning: Boolean = false,
    val isCompleted: Boolean = false,
    val isInterrupted: Boolean = false,
    val playlistId: String? = null,
    val playlistName: String = "",
    val totalChannels: Int = 0,
    val verifiedChannels: Int = 0,
    val currentChannelName: String = "",
    val onlineCount: Int = 0,
    val offlineCount: Int = 0,
    val indeterminateCount: Int = 0,
    val checkingCount: Int = 0,
    val progressFraction: Float = 0f,
    val message: String? = null
)

/**
 * Internal pending write item for batched Room commits.
 */
data class ChannelStatusUpdate(
    val channelId: String,
    val status: StreamStatus,
    val latencyMs: Long?,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Core engine responsible for executing the automatic channel verification queue.
 * Optimized for HY300 hardware:
 * - Default maximum 3 simultaneous checks (configurable)
 * - Controlled async queue using Kotlin Coroutine Channels
 * - Batched Room updates to avoid flash memory wear and UI stutter
 * - Graceful stop with result preservation
 */
class StreamCheckerEngine(
    private val database: AppDatabase,
    private val streamVerifier: StreamVerifier = StreamVerifier(),
    private val defaultMaxConcurrency: Int = 3
) {
    private val channelDao = database.channelDao()
    private val engineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _progressState = MutableStateFlow(CheckerProgressState())
    val progressState: StateFlow<CheckerProgressState> = _progressState.asStateFlow()

    private var activeJob: Job? = null
    private val isCancelled = AtomicBoolean(false)

    // Batch writing buffer
    private val pendingBuffer = mutableListOf<ChannelStatusUpdate>()
    private val bufferMutex = Mutex()
    private val BATCH_SIZE = 5

    /**
     * Starts or restarts automatic verification for the given playlist.
     */
    fun startVerification(
        playlistId: String,
        playlistName: String,
        maxConcurrency: Int = defaultMaxConcurrency
    ) {
        if (_progressState.value.isRunning) {
            return
        }

        isCancelled.set(false)
        activeJob = engineScope.launch {
            runVerification(playlistId, playlistName, maxConcurrency)
        }
    }

    /**
     * Safely stops the verification queue.
     * Prevents starting new checks, allows active checks to finish/cancel safely,
     * flushes pending database writes, and marks state as interrupted.
     */
    fun stopVerification() {
        if (!_progressState.value.isRunning) return
        isCancelled.set(true)
        activeJob?.cancel()
        activeJob = null

        engineScope.launch {
            flushBuffer()
            _progressState.update { current ->
                current.copy(
                    isRunning = false,
                    isInterrupted = true,
                    message = "Verificação interrompida."
                )
            }
        }
    }

    private suspend fun runVerification(
        playlistId: String,
        playlistName: String,
        concurrency: Int
    ) = withContext(Dispatchers.IO) {
        val channels = channelDao.getChannelsByPlaylistId(playlistId)
        val total = channels.size

        if (total == 0) {
            _progressState.value = CheckerProgressState(
                isRunning = false,
                isCompleted = true,
                playlistId = playlistId,
                playlistName = playlistName,
                totalChannels = 0,
                message = "A playlist selecionada não tem canais."
            )
            return@withContext
        }

        // 1. Reset all channels in playlist to CHECKING status in a single instant query
        channelDao.resetChannelStatusesForPlaylist(playlistId, StreamStatus.CHECKING.name)

        val onlineCounter = AtomicInteger(0)
        val offlineCounter = AtomicInteger(0)
        val indeterminateCounter = AtomicInteger(0)
        val verifiedCounter = AtomicInteger(0)

        _progressState.value = CheckerProgressState(
            isRunning = true,
            isCompleted = false,
            isInterrupted = false,
            playlistId = playlistId,
            playlistName = playlistName,
            totalChannels = total,
            verifiedChannels = 0,
            onlineCount = 0,
            offlineCount = 0,
            indeterminateCount = 0,
            checkingCount = total,
            progressFraction = 0f,
            message = "A verificar..."
        )

        // 2. Controlled asynchronous queue with capacity
        val queue = Channel<ChannelEntity>(capacity = Channel.UNLIMITED)
        channels.forEach { queue.trySend(it) }
        queue.close()

        val actualConcurrency = concurrency.coerceIn(1, 5)
        val workers = List(actualConcurrency) {
            launch {
                for (channel in queue) {
                    if (isCancelled.get()) break

                    // Update currently verifying channel in state
                    _progressState.update { current ->
                        current.copy(
                            currentChannelName = channel.name,
                            checkingCount = (total - verifiedCounter.get()).coerceAtLeast(0)
                        )
                    }

                    // Perform real network/HLS verification (4s timeout)
                    val result = streamVerifier.verify(channel.streamUrl)

                    when (result.status) {
                        StreamStatus.ONLINE -> onlineCounter.incrementAndGet()
                        StreamStatus.OFFLINE -> offlineCounter.incrementAndGet()
                        StreamStatus.UNKNOWN -> indeterminateCounter.incrementAndGet()
                        StreamStatus.CHECKING -> {}
                    }

                    val verifiedSoFar = verifiedCounter.incrementAndGet()
                    val fraction = verifiedSoFar.toFloat() / total.toFloat()

                    // Update UI state in real time
                    _progressState.update { current ->
                        current.copy(
                            verifiedChannels = verifiedSoFar,
                            onlineCount = onlineCounter.get(),
                            offlineCount = offlineCounter.get(),
                            indeterminateCount = indeterminateCounter.get(),
                            checkingCount = (total - verifiedSoFar).coerceAtLeast(0),
                            progressFraction = fraction
                        )
                    }

                    // Buffer database update
                    bufferUpdate(
                        ChannelStatusUpdate(
                            channelId = channel.id,
                            status = result.status,
                            latencyMs = result.latencyMs,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

        // Wait for all workers to finish or handle cancellation
        workers.joinAll()

        // Flush all pending database writes
        flushBuffer()

        if (!isCancelled.get()) {
            _progressState.update { current ->
                current.copy(
                    isRunning = false,
                    isCompleted = true,
                    isInterrupted = false,
                    checkingCount = 0,
                    progressFraction = 1.0f,
                    message = "Verificação concluída com sucesso."
                )
            }
        }
    }

    private suspend fun bufferUpdate(update: ChannelStatusUpdate) {
        val shouldFlush = bufferMutex.withLock {
            pendingBuffer.add(update)
            pendingBuffer.size >= BATCH_SIZE
        }
        if (shouldFlush) {
            flushBuffer()
        }
    }

    private suspend fun flushBuffer() {
        val batch = bufferMutex.withLock {
            if (pendingBuffer.isEmpty()) return
            val copy = ArrayList(pendingBuffer)
            pendingBuffer.clear()
            copy
        }

        withContext(Dispatchers.IO) {
            for (item in batch) {
                channelDao.updateChannelStatus(
                    channelId = item.channelId,
                    status = item.status.name,
                    latency = item.latencyMs,
                    timestamp = item.timestamp
                )
            }
        }
    }
}
