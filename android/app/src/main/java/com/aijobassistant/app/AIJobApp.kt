package com.aijobassistant.app

import android.app.Application
import com.google.firebase.FirebaseApp

/**
 * Application class for AI Job Assistant.
 * Initializes Firebase on app startup.
 */
class AIJobApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
