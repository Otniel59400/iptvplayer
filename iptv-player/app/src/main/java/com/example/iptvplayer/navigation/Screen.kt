package com.example.iptvplayer.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CastConnected
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tv
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector? = null
) {
    object Splash : Screen("splash", R.string.app_name)
    object Home : Screen("home", R.string.nav_home, Icons.Default.Home)
    object Playlists : Screen("playlists", R.string.nav_playlists, Icons.Default.PlaylistPlay)
    object Channels : Screen("channels", R.string.nav_channels, Icons.Default.Tv)
    object Favorites : Screen("favorites", R.string.nav_favorites, Icons.Default.Favorite)
    object Users : Screen("users", R.string.nav_users, Icons.Default.ManageAccounts)
    object Checker : Screen("checker", R.string.nav_checker, Icons.Default.CheckCircleOutline)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Default.Settings)
    object Player : Screen("player", R.string.action_play, Icons.Default.Tv)
    val PlayerPreview = Player
    object SelectProfile : Screen("select_profile", R.string.profile_select_title)
    object SetupFirstProfile : Screen("setup_first_profile", R.string.setup_first_profile_title)

    companion object {
        val navItems = listOf(
            Home,
            Playlists,
            Channels,
            Favorites,
            Users,
            Checker,
            Settings
        )
    }
}
