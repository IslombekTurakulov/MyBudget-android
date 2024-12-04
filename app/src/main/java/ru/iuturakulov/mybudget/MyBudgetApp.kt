package ru.iuturakulov.mybudget

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class MyBudgetApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Инициализация Timber
        Timber.plant(Timber.DebugTree())
    }
}