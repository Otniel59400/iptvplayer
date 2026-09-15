package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.iptvplayer.data.database.AppDatabase
import com.example.iptvplayer.data.database.ChannelEntity
import com.example.iptvplayer.data.database.PlaylistEntity
import com.example.iptvplayer.data.parser.M3UParserImpl
import com.example.iptvplayer.data.repository.ChannelRepositoryImpl
import com.example.iptvplayer.data.repository.PlaylistRepositoryImpl
import com.example.iptvplayer.data.repository.UserRepositoryImpl
import com.example.iptvplayer.navigation.Screen
import com.example.iptvplayer.ui.home.HomeScreen
import com.example.iptvplayer.ui.home.HomeViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
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
class HomeScreenFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var playlistRepository: PlaylistRepositoryImpl
    private lateinit var channelRepository: ChannelRepositoryImpl
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var testScope: CoroutineScope

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
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

        val streamCheckerRepository = com.example.iptvplayer.data.repository.StreamCheckerRepositoryImpl(db)

        homeViewModel = HomeViewModel(
            playlistRepository = playlistRepository,
            channelRepository = channelRepository,
            userRepository = userRepository,
            streamCheckerRepository = streamCheckerRepository
        )
    }

    @After
    fun tearDown() {
        testScope.cancel()
        db.close()
    }

    @Test
    fun homeScreen_emptyPlaylists_showsEmptyStateAndAddButton() {
        runBlocking {
            val userResult = userRepository.createUser(
                username = "TestUser",
                displayName = "Familia Teste",
                pinOrPassword = null
            )
            assertTrue(userResult.isSuccess)

            var addPlaylistClicked = false

            composeTestRule.setContent {
                MyApplicationTheme {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigate = {},
                        onNavigateToAddPlaylist = { addPlaylistClicked = true },
                        onOpenPlaylistChannels = {},
                        onPlayChannel = {}
                    )
                }
            }

            // Verify Home screen content container is displayed
            composeTestRule.onNodeWithTag("home_screen_content").assertIsDisplayed()

            // Verify Hero card is displayed with profile info
            composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()

            // Verify Empty playlists guidance card is displayed
            composeTestRule.onNodeWithTag("home_empty_playlists_guidance_card").assertIsDisplayed()

            // Verify CTA button "+ Adicionar lista M3U" is displayed and clickable
            val ctaBtn = composeTestRule.onNodeWithTag("btn_empty_home_add_playlist")
            ctaBtn.assertIsDisplayed()
            ctaBtn.performClick()
            assertTrue(addPlaylistClicked)
        }
    }

    @Test
    fun homeScreen_withPlaylistsAndChannels_displaysDataDrivenDashboard() {
        runBlocking {
            // Create user
            val userResult = userRepository.createUser(
                username = "Maria",
                displayName = "Maria Silva",
                pinOrPassword = null
            )
            val user = userResult.getOrThrow()

            val playlistId = UUID.randomUUID().toString()
            val playlistEntity = PlaylistEntity(
                id = playlistId,
                userId = user.id,
                name = "Canais de Portugal",
                type = "LOCAL",
                source = "/path/playlist.m3u",
                url = null,
                channelCount = 2,
                orderIndex = 0
            )
            db.playlistDao().insert(playlistEntity)

            // Add Channels
            val ch1 = ChannelEntity(
                id = UUID.randomUUID().toString(),
                playlistId = playlistId,
                userId = user.id,
                name = "RTP 1 HD",
                streamUrl = "https://example.com/rtp1.m3u8",
                groupTitle = "Geral",
                status = "ONLINE",
                isFavorite = true,
                lastPlayedAt = System.currentTimeMillis() - 1000
            )
            val ch2 = ChannelEntity(
                id = UUID.randomUUID().toString(),
                playlistId = playlistId,
                userId = user.id,
                name = "SIC Noticias",
                streamUrl = "https://example.com/sic.m3u8",
                groupTitle = "Notícias",
                status = "ONLINE",
                isFavorite = false,
                lastPlayedAt = System.currentTimeMillis()
            )
            db.channelDao().insertAll(listOf(ch1, ch2))

            var quickActionChannelsClicked = false

            composeTestRule.setContent {
                MyApplicationTheme {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigate = { screen ->
                            if (screen == Screen.Channels) {
                                quickActionChannelsClicked = true
                            }
                        },
                        onNavigateToAddPlaylist = {},
                        onOpenPlaylistChannels = {},
                        onPlayChannel = {}
                    )
                }
            }

            // Verify Home content is displayed
            composeTestRule.onNodeWithTag("home_screen_content").assertIsDisplayed()
            composeTestRule.onNodeWithTag("home_hero_card").assertIsDisplayed()

            // Verify Nav action cards are displayed and interactive
            composeTestRule.onNodeWithTag("nav_card_add_playlist", useUnmergedTree = true).assertIsDisplayed()
            val channelsAction = composeTestRule.onNodeWithTag("nav_card_channels", useUnmergedTree = true)
            channelsAction.assertIsDisplayed()
            channelsAction.performClick()
            assertTrue(quickActionChannelsClicked)
        }
    }

    @Test
    fun homeScreen_userIsolation_showsOnlyActiveUserData() {
        runBlocking {
            // User 1
            val user1Result = userRepository.createUser(
                username = "User1",
                displayName = "Perfil Um",
                pinOrPassword = null
            )
            val user1 = user1Result.getOrThrow()

            // User 2
            val user2Result = userRepository.createUser(
                username = "User2",
                displayName = "Perfil Dois",
                pinOrPassword = null
            )
            val user2 = user2Result.getOrThrow()

            // Add playlist for User 1 only
            val playlist1 = PlaylistEntity(
                id = UUID.randomUUID().toString(),
                userId = user1.id,
                name = "Lista User 1",
                type = "LOCAL",
                source = "/path/u1.m3u",
                url = null,
                channelCount = 5,
                orderIndex = 0
            )
            db.playlistDao().insert(playlist1)

            // Login User 2 (who has 0 playlists)
            userRepository.login(user2.id, null)

            composeTestRule.setContent {
                MyApplicationTheme {
                    HomeScreen(
                        viewModel = homeViewModel,
                        onNavigate = {},
                        onNavigateToAddPlaylist = {},
                        onOpenPlaylistChannels = {},
                        onPlayChannel = {}
                    )
                }
            }

            // User 2 should see empty playlists guidance card
            composeTestRule.onNodeWithTag("home_empty_playlists_guidance_card").assertIsDisplayed()
        }
    }
}
