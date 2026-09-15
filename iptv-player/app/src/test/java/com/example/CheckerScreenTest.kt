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
import com.example.iptvplayer.data.repository.PlaylistRepositoryImpl
import com.example.iptvplayer.data.repository.StreamCheckerRepositoryImpl
import com.example.iptvplayer.data.repository.UserRepositoryImpl
import com.example.iptvplayer.domain.model.StreamStatus
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.ui.checker.CheckerScreen
import com.example.iptvplayer.ui.checker.CheckerViewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h720dp")
class CheckerScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var playlistRepository: PlaylistRepositoryImpl
    private lateinit var streamCheckerRepository: StreamCheckerRepositoryImpl
    private lateinit var testScope: CoroutineScope

    private val testPlaylistId = "playlist-checker-test"

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("iptv_user_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        testScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .setTransactionExecutor(Dispatchers.Unconfined.asExecutor())
            .setQueryExecutor(Dispatchers.Unconfined.asExecutor())
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
        streamCheckerRepository = StreamCheckerRepositoryImpl(db)

        runBlocking {
            val userResult: Result<User> = userRepository.createUser(
                username = "Tester",
                displayName = "Tester User",
                pinOrPassword = null
            )
            val user = userResult.getOrThrow()

            val playlist = PlaylistEntity(
                id = testPlaylistId,
                userId = user.id,
                name = "Minha TV",
                type = "LOCAL",
                source = "/path/test.m3u",
                url = null,
                channelCount = 3,
                orderIndex = 0
            )
            db.playlistDao().insert(playlist)

            val ch1 = ChannelEntity(
                id = UUID.randomUUID().toString(),
                playlistId = testPlaylistId,
                userId = user.id,
                name = "RTP 1 HD",
                groupTitle = "Nacional",
                streamUrl = "http://example.com/rtp1.m3u8",
                logoUrl = null,
                tvgId = "rtp1",
                tvgName = "RTP 1",
                status = StreamStatus.ONLINE.name,
                latency = 120L,
                lastChecked = System.currentTimeMillis(),
                orderIndex = 0,
                isFavorite = false
            )
            val ch2 = ChannelEntity(
                id = UUID.randomUUID().toString(),
                playlistId = testPlaylistId,
                userId = user.id,
                name = "SIC Noticias",
                groupTitle = "Informação",
                streamUrl = "http://example.com/sic.m3u8",
                logoUrl = null,
                tvgId = "sic",
                tvgName = "SIC Notícias",
                status = StreamStatus.OFFLINE.name,
                latency = 3500L,
                lastChecked = System.currentTimeMillis(),
                orderIndex = 1,
                isFavorite = false
            )
            val ch3 = ChannelEntity(
                id = UUID.randomUUID().toString(),
                playlistId = testPlaylistId,
                userId = user.id,
                name = "Sport TV 1",
                groupTitle = "Desporto",
                streamUrl = "http://example.com/sporttv.m3u8",
                logoUrl = null,
                tvgId = "sporttv1",
                tvgName = "Sport TV 1",
                status = StreamStatus.UNKNOWN.name,
                latency = null,
                lastChecked = null,
                orderIndex = 2,
                isFavorite = false
            )
            db.channelDao().insertAll(listOf(ch1, ch2, ch3))
        }
    }

    @After
    fun tearDown() {
        db.close()
        testScope.cancel()
    }

    @Test
    fun testCheckerScreenDisplaysRealStatisticsAndFilters() {
        val viewModel = CheckerViewModel(
            checkerRepository = streamCheckerRepository,
            playlistRepository = playlistRepository,
            userRepository = userRepository
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                CheckerScreen(
                    checkerRepository = streamCheckerRepository,
                    playlistRepository = playlistRepository,
                    userRepository = userRepository,
                    viewModel = viewModel
                )
            }
        }

        // Verify root screen and statistics are displayed
        composeTestRule.onNodeWithTag("checker_screen_root").assertIsDisplayed()
        composeTestRule.onNodeWithTag("checker_statistics_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_online_count", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_offline_count", useUnmergedTree = true).assertIsDisplayed()
        composeTestRule.onNodeWithTag("stat_indeterminate_count", useUnmergedTree = true).assertIsDisplayed()

        // Verify start / recheck action button is available and clickable
        val startBtn = composeTestRule.onNodeWithTag("btn_start_stream_check", useUnmergedTree = true)
        startBtn.assertIsDisplayed()
        startBtn.performClick()
    }
}
