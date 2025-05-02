package ru.iuturakulov.mybudget.auth

import android.content.SharedPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CodeTokenStorage @Inject constructor(
    val encryptedSharedPreferences: SharedPreferences
) {
    private val inviteCodeToken = MutableStateFlow<String?>(null)

    fun getCodeToken(): String? = inviteCodeToken.value

    fun getCodeTokenFlow(): Flow<String?> = inviteCodeToken.asStateFlow()

    fun getLocale(): String? = encryptedSharedPreferences.getString("locale", null)

    suspend fun saveCodeTokenAsync(newToken: String) {
        withContext(Dispatchers.IO) { saveCodeToken(newToken) }
    }

    fun saveCodeToken(newToken: String) {
        if (inviteCodeToken.value != newToken) {
            inviteCodeToken.value = newToken
        }
    }

    fun clearCodeToken() {
        inviteCodeToken.value = null
    }
}
