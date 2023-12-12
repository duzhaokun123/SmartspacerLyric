package io.github.duzhaokun123.smartspacerlyric

import android.content.IntentFilter

class Application: android.app.Application() {
    override fun onCreate() {
        super.onCreate()
        registerReceiver(LyricReceiver(), IntentFilter("Lyric_Data"), RECEIVER_EXPORTED)
    }
}