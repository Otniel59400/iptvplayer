package com.example

import android.app.Application
import com.example.iptvplayer.di.AppContainer

class IPTVApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
