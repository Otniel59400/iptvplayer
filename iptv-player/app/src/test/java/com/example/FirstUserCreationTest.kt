package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.iptvplayer.data.database.AppDatabase
import com.example.iptvplayer.data.repository.UserRepositoryImpl
import com.example.iptvplayer.domain.model.User
import com.example.iptvplayer.ui.users.UserViewModel
import com.example.iptvplayer.ui.users.login.SelectProfileScreen
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w1280dp-h720dp")
class FirstUserCreationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var db: AppDatabase
    private lateinit var userRepository: UserRepositoryImpl
    private lateinit var viewModel: UserViewModel
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
        viewModel = UserViewModel(
            userRepository = userRepository,
            coroutineScope = testScope
        )
    }

    @After
    fun tearDown() {
        testScope.cancel()
        db.close()
    }

    @Test
    fun zeroUsersState_displaysCleanWelcomeScreen_withCreateUserButton() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SelectProfileScreen(
                    viewModel = viewModel,
                    canGoBack = false,
                    onNavigateBack = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Must display Welcome and subtitle
        composeTestRule.onNodeWithText("Bem-vindo ao IPTV Player").assertIsDisplayed()
        composeTestRule.onNodeWithText("Crie o primeiro utilizador para começar.").assertIsDisplayed()

        // Must display the large primary "Criar utilizador" button
        composeTestRule.onNodeWithTag("button_create_user_primary").assertIsDisplayed()

        // Must NOT show sample users or secondary messages
        composeTestRule.onNodeWithText("Otniel").assertDoesNotExist()
        composeTestRule.onNodeWithText("Família").assertDoesNotExist()
        composeTestRule.onNodeWithText("Convidado").assertDoesNotExist()
        composeTestRule.onNodeWithText("Quem está a assistir?").assertDoesNotExist()
    }

    @Test
    fun clickCreateUser_opensForm_andAllowsCancellation() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SelectProfileScreen(
                    viewModel = viewModel,
                    canGoBack = false,
                    onNavigateBack = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Click "Criar utilizador"
        composeTestRule.onNodeWithTag("button_create_user_primary").performClick()

        // Form fields must be displayed
        composeTestRule.onNodeWithTag("first_user_creation_form").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_first_user_name").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_first_user_pin").assertIsDisplayed()
        composeTestRule.onNodeWithTag("input_first_user_pin_confirm").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_submit_first_user").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_back_first_user").assertIsDisplayed()

        // Click "Voltar"
        composeTestRule.onNodeWithTag("button_back_first_user").performClick()

        // Returns to Welcome screen
        composeTestRule.onNodeWithText("Bem-vindo ao IPTV Player").assertIsDisplayed()
        composeTestRule.onNodeWithTag("button_create_user_primary").assertIsDisplayed()
    }

    @Test
    fun formValidation_detectsEmptyUsername_andPinMismatch() {
        composeTestRule.setContent {
            MyApplicationTheme {
                SelectProfileScreen(
                    viewModel = viewModel,
                    canGoBack = false,
                    onNavigateBack = {},
                    onLoginSuccess = {}
                )
            }
        }

        // Open form
        composeTestRule.onNodeWithTag("button_create_user_primary").performClick()

        // 1. Submit empty -> expects empty name error
        composeTestRule.onNodeWithTag("button_submit_first_user").performClick()
        composeTestRule.onNodeWithText("O nome do utilizador não pode estar vazio").assertIsDisplayed()

        // 2. Enter username, leave PIN empty
        composeTestRule.onNodeWithTag("input_first_user_name").performTextInput("António")
        composeTestRule.onNodeWithTag("button_submit_first_user").performClick()
        composeTestRule.onNodeWithText("Introduza um PIN ou palavra-passe").assertIsDisplayed()

        // 3. Enter short PIN (< 4 digits)
        composeTestRule.onNodeWithTag("input_first_user_pin").performTextInput("12")
        composeTestRule.onNodeWithTag("button_submit_first_user").performClick()
        composeTestRule.onNodeWithText("PIN ou palavra-passe inválido. Introduza entre 4 e 8 dígitos").assertIsDisplayed()

        // 4. Enter PIN mismatch
        composeTestRule.onNodeWithTag("input_first_user_pin").performTextInput("34") // now "1234"
        composeTestRule.onNodeWithTag("input_first_user_pin_confirm").performTextInput("9999")
        composeTestRule.onNodeWithTag("button_submit_first_user").performClick()
        composeTestRule.onNodeWithText("O PIN ou palavra-passe não coincide").assertIsDisplayed()
    }

    @Test
    fun successfulCreation_createsUser_andTriggersLoginSuccess() {
        var loggedInUser: User? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                SelectProfileScreen(
                    viewModel = viewModel,
                    canGoBack = false,
                    onNavigateBack = {},
                    onLoginSuccess = { user ->
                        loggedInUser = user
                    }
                )
            }
        }

        // Open form
        composeTestRule.onNodeWithTag("button_create_user_primary").performClick()

        // Fill valid credentials
        composeTestRule.onNodeWithTag("input_first_user_name").performTextInput("Manuel")
        composeTestRule.onNodeWithTag("input_first_user_pin").performTextInput("1234")
        composeTestRule.onNodeWithTag("input_first_user_pin_confirm").performTextInput("1234")

        // Submit form
        composeTestRule.onNodeWithTag("button_submit_first_user").performScrollTo().performClick()

        // Wait for coroutine to complete and login callback to be invoked
        composeTestRule.waitUntil(5000) { loggedInUser != null }

        // Verify login success callback was called with the new user
        assertNotNull(loggedInUser)
        assertEquals("Manuel", loggedInUser?.username)
        assertTrue(loggedInUser?.isPinProtected == true)
    }
}
