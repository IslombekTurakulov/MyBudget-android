package ru.iuturakulov.mybudget

import android.app.Application
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyBudgetApp : Application() {

    override fun onCreate() {
        super.onCreate()

        EmojiManager.install(GoogleEmojiProvider())
        // Инициализация Timber
        Timber.plant(Timber.DebugTree())
    }
}