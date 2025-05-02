package ru.iuturakulov.mybudget

import android.app.Application
import com.airbnb.lottie.LottieCompositionFactory
import com.airbnb.lottie.model.LottieCompositionCache
import com.vanniktech.emoji.EmojiManager
import com.vanniktech.emoji.google.GoogleEmojiProvider
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import kotlin.concurrent.thread

@HiltAndroidApp
class MyBudgetApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Синхронно распарсим и кэшируем composition
        Thread {
            LottieCompositionFactory.fromRawResSync(this, R.raw.lottie_safe_money)
        }.start()

        // Инициализация Timber
        Timber.plant(Timber.DebugTree())
    }
}