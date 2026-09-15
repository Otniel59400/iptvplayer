package com.example

import android.app.Application
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.iptvplayer.data.database.AppDatabase
import com.example.iptvplayer.data.database.ChannelEntity
import com.example.iptvplayer.data.database.PlaylistEntity
import com.example.iptvplayer.data.parser.M3UParserImpl
import com.example.iptvplayer.data.repository.ChannelRepositoryImpl
import com.example.iptvplayer.data.repository.PlaylistRepositoryImpl
import com.example.iptvplayer.data.repository.UserRepositoryImpl
import com.example.iptvplayer.domain.model.Channel
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.ui.player.PlayerControlsOverlay
import com.example.iptvplayer.ui.player.PlayerPlaybackState
import com.example.iptvplayer.ui.player.PlayerViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h720dp")
class PlayerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var playlistRepository: PlaylistRepositoryImpl
    private lateinit var channelRepository: ChannelRepositoryImpl
    private lateinit var playerViewModel: PlayerViewModel
    private lateinit var testScope: CoroutineScope

    private val testChannel = Channel(
        id = "channel-101",
        playlistId = "playlist-1",
        name = "RTP 1 HD",
        streamUrl = "https://example.com/live/rtp1.m3u8",
        logoUrl = null,
        groupTitle = "Geral",
        tvgId = "rtp1",
        orderIndex = 0,
        isFavorite = false,
        status = StreamStatus.ONLINE
    )

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        context.getSharedPreferences("iptv_user_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .setQueryExecutor(Dispatchers.Unconfined.asExecutor())
            .setTransactionExecutor(Dispatchers.Unconfined.asExecutor())
            .allowMainThreadQueries()
            .build()

        userRepository = UserRepositoryImpl(
            userDao = db.userDao(),
            context = context,
            ioDispatcher = Dispatchers.Unconfined
        )

        val m3uParser = M3UParserImpl()

        playlistRepository = PlaylistRepositoryImpl(
            database = db,
            m3uParser = m3uParser,
            userRepository = userRepository
        )

        channelRepository = ChannelRepositoryImpl(
            database = db,
            userRepository = userRepository
        )

        playerViewModel = PlayerViewModel(
            application = context as Application,
            channelRepository = channelRepository,
            userRepository = userRepository,
            playlistRepository = playlistRepository
        )
    }

    @After
    fun tearDown() {
        testScope.cancel()
        db.close()
    }

    @Test
    fun playerControlsOverlay_rendersBottomLeftChannelListButton() {
        var channelListClicked = false
        var backClicked = false
        var playPauseClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                PlayerControlsOverlay(
                    isVisible = true,
                    channel = testChannel,
                    playbackState = PlayerPlaybackState.Playing,
                    isLiveStream = true,
                    liveOffsetMs = 1200L,
                    isFavorite = false,
                    isMuted = false,
                    isAspectRatioFit = true,
                    onTogglePlayPause = { playPauseClicked = true },
                    onToggleFavorite = {},
                    onToggleMute = {},
                    onToggleAspectRatio = {},
                    onRetry = {},
                    onOpenChannelOverlay = { channelListClicked = true },
                    onBackClick = { backClicked = true },
                    onUserInteraction = {}
                )
            }
        }

        // Verify channel information is displayed
        composeTestRule.onNodeWithText("RTP 1 HD").assertIsDisplayed()
        composeTestRule.onNodeWithText("Geral").assertIsDisplayed()

        // Verify live edge badge is displayed
        composeTestRule.onNodeWithTag("player_live_delay_badge").assertIsDisplayed()

        // Verify bottom-left "Lista de Canais" button exists and triggers callback
        val channelListBtn = composeTestRule.onNodeWithTag("player_btn_channel_list")
        channelListBtn.assertIsDisplayed()
        channelListBtn.performClick()
        assertTrue(channelListClicked)

        // Verify Center Play/Pause button exists and triggers callback
        val playPauseBtn = composeTestRule.onNodeWithTag("player_center_play_pause_btn")
        playPauseBtn.assertIsDisplayed()
        playPauseBtn.performClick()
        assertTrue(playPauseClicked)

        // Verify Back button exists and triggers callback
        val backBtn = composeTestRule.onNodeWithTag("player_btn_back")
        backBtn.assertIsDisplayed()
        backBtn.performClick()
        assertTrue(backClicked)
    }

    @Test
    fun playerControlsOverlay_bufferingState_showsPortugueseBufferingIndicator() {
        composeTestRule.setContent {
            MyApplicationTheme {
                PlayerControlsOverlay(
                    isVisible = true,
                    channel = testChannel,
                    playbackState = PlayerPlaybackState.Buffering(message = "Buffering…"),
                    isLiveStream = true,
                    liveOffsetMs = null,
                    isFavorite = false,
                    isMuted = false,
                    isAspectRatioFit = true,
                    onTogglePlayPause = {},
                    onToggleFavorite = {},
                    onToggleMute = {},
                    onToggleAspectRatio = {},
                    onRetry = {},
                    onOpenChannelOverlay = {},
                    onBackClick = {},
                    onUserInteraction = {}
                )
            }
        }

        // Verify buffering state banner is displayed with Portuguese text
        composeTestRule.onNodeWithTag("player_state_buffering").assertIsDisplayed()
        composeTestRule.onNodeWithText("Buffering…").assertIsDisplayed()
    }

    @Test
    fun playerControlsOverlay_reconnectingState_showsReconnectingBannerWithAttempts() {
        composeTestRule.setContent {
            MyApplicationTheme {
                PlayerControlsOverlay(
                    isVisible = true,
                    channel = testChannel,
                    playbackState = PlayerPlaybackState.Reconnecting(attempt = 2, maxAttempts = 3),
                    isLiveStream = true,
                    liveOffsetMs = null,
                    isFavorite = false,
                    isMuted = false,
                    isAspectRatioFit = true,
                    onTogglePlayPause = {},
                    onToggleFavorite = {},
                    onToggleMute = {},
                    onToggleAspectRatio = {},
                    onRetry = {},
                    onOpenChannelOverlay = {},
                    onBackClick = {},
                    onUserInteraction = {}
                )
            }
        }

        // Verify reconnecting banner with Portuguese attempt info is displayed
        composeTestRule.onNodeWithTag("player_state_reconnecting").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reconectando…").assertIsDisplayed()
        composeTestRule.onNodeWithText("Reconectando (2/3)…").assertIsDisplayed()
    }

    @Test
    fun playerControlsOverlay_errorState_showsPortugueseErrorMessageAndRetryButton() {
        var retryClicked = false

        composeTestRule.setContent {
            MyApplicationTheme {
                PlayerControlsOverlay(
                    isVisible = true,
                    channel = testChannel,
                    playbackState = PlayerPlaybackState.Error("Stream offline"),
                    isLiveStream = false,
                    liveOffsetMs = null,
                    isFavorite = false,
                    isMuted = false,
                    isAspectRatioFit = true,
                    onTogglePlayPause = {},
                    onToggleFavorite = {},
                    onToggleMute = {},
                    onToggleAspectRatio = {},
                    onRetry = { retryClicked = true },
                    onOpenChannelOverlay = {},
                    onBackClick = {},
                    onUserInteraction = {}
                )
            }
        }

        // Verify error card is displayed with Portuguese prompt
        composeTestRule.onNodeWithTag("player_state_error").assertIsDisplayed()
        composeTestRule.onNodeWithText("Não foi possível reproduzir este canal.").assertIsDisplayed()

        // Verify "Repetir" button functions
        val retryBtn = composeTestRule.onNodeWithTag("player_btn_error_retry")
        retryBtn.assertIsDisplayed()
        retryBtn.performClick()
        assertTrue(retryClicked)
    }

    @Test
    fun playerViewModel_overlayState_initialValuesAndCategorySelection() {
        assertEquals("Todos", playerViewModel.selectedOverlayCategory.value)
        assertEquals("", playerViewModel.overlaySearchQuery.value)
        assertEquals(false, playerViewModel.isChannelOverlayOpen.value)

        playerViewModel.openChannelOverlay()
        assertEquals(true, playerViewModel.isChannelOverlayOpen.value)

        playerViewModel.setOverlaySearchQuery("RTP")
        assertEquals("RTP", playerViewModel.overlaySearchQuery.value)

        playerViewModel.selectOverlayCategory("Notícias")
        assertEquals("Notícias", playerViewModel.selectedOverlayCategory.value)

        playerViewModel.closeChannelOverlay()
        assertEquals(false, playerViewModel.isChannelOverlayOpen.value)
    }
}
