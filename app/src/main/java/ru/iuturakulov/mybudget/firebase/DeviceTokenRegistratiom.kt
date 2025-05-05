package ru.iuturakulov.mybudget.firebase

import androidx.annotation.WorkerThread
import kotlinx.coroutines.*
import ru.iuturakulov.mybudget.data.remote.FCMService
import ru.iuturakulov.mybudget.data.remote.dto.RegisterDeviceRequest
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeviceTokenRegistratiom @Inject constructor(
    private val fcmService: FCMService
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var lastToken: String? = null

    @WorkerThread
    fun enqueue(token: String, languageCode: String) {
        if (token == lastToken) return
        lastToken = token

        scope.launch {
            try {
                val response = fcmService.registerDevice(RegisterDeviceRequest(token, language = languageCode))
                if (!response.isSuccessful) {
                    throw Exception(response.errorBody().toString())
                }
                Timber.i("Device registered successfully with token $lastToken")
            } catch (e: Exception) {
                Timber.e("Failed to register device with token $token. Info: $e")
            }
        }
    }
}
