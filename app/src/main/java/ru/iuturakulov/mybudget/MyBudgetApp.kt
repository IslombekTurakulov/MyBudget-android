package ru.iuturakulov.mybudget

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.airbnb.lottie.LottieCompositionFactory
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.HiltAndroidApp
import ru.iuturakulov.mybudget.di.PreferencesEntryPoint
import ru.iuturakulov.mybudget.firebase.DeviceTokenRegistratiom
import ru.iuturakulov.mybudget.firebase.MyFirebaseMessagingService
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

@HiltAndroidApp
class MyBudgetApp : Application() {

    @Inject
    lateinit var deviceTokenRegistratiom: DeviceTokenRegistratiom

    override fun onCreate() {
        super.onCreate()

        // Синхронно распарсим и кэшируем composition
        Thread {
            LottieCompositionFactory.fromRawResSync(this, R.raw.lottie_safe_money)
        }.start()

        createNotificationChannel()
        // сразу получить уже существующий токен (если был)
        FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
            val prefs = EntryPointAccessors.fromApplication(
                applicationContext,
                PreferencesEntryPoint::class.java
            ).encryptedPrefs()

            val languageCode = prefs.getString(
                "locale",
                Locale.getDefault().language
            ) ?: Locale.getDefault().language

            deviceTokenRegistratiom.enqueue(token, languageCode)
        }

        // Инициализация Timber
        Timber.plant(Timber.DebugTree())
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            MyFirebaseMessagingService.CHANNEL_ID,
            "Уведомления Мой Бюджет",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }
}