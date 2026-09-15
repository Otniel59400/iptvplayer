package com.example.iptvplayer.di

import android.content.Context
import com.example.iptvplayer.data.database.AppDatabase
import com.example.iptvplayer.data.parser.M3UParser
import com.example.iptvplayer.data.parser.M3UParserImpl
import com.example.iptvplayer.data.repository.ChannelRepositoryImpl
import com.example.iptvplayer.data.repository.PlaylistRepositoryImpl
import com.example.iptvplayer.data.repository.StreamCheckerRepositoryImpl
import com.example.iptvplayer.data.repository.UserRepositoryImpl
import com.example.iptvplayer.domain.repository.ChannelRepository
import com.example.iptvplayer.domain.repository.PlaylistRepository
import com.example.iptvplayer.domain.repository.StreamCheckerRepository
import com.example.iptvplayer.domain.repository.UserRepository

/**
 * Lightweight dependency container providing Room database and repository instances.
 */
class AppContainer(context: Context) {
    val database: AppDatabase = AppDatabase.getInstance(context)

    val userRepository: UserRepository by lazy {
        UserRepositoryImpl(
            userDao = database.userDao(),
            context = context.applicationContext
        )
    }

    val m3uParser: M3UParser by lazy {
        M3UParserImpl()
    }

    val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepositoryImpl(
            database = database,
            m3uParser = m3uParser,
            userRepository = userRepository
        )
    }

    val channelRepository: ChannelRepository by lazy {
        ChannelRepositoryImpl(
            database = database,
            userRepository = userRepository
        )
    }

    val streamCheckerRepository: StreamCheckerRepository by lazy {
        StreamCheckerRepositoryImpl(database = database)
    }
}
