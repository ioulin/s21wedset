package com.example.flogem

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class FloGemApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
