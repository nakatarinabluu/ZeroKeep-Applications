package com.vaultguard.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class VaultGuardApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Setup "Number 5": Centralized Error Handling
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler(
                com.vaultguard.app.utils.GlobalExceptionHandler(this, defaultHandler)
        )
    }
}
