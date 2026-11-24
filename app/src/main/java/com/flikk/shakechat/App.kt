package com.flikk.shakechat

import android.app.Application
import com.google.firebase.FirebaseApp

class App : Application() {
    companion object {
        var firebaseReady = false
            private set
    }

    override fun onCreate() {
        super.onCreate()
        firebaseReady = FirebaseApp.getApps(this).isNotEmpty()
    }
}
