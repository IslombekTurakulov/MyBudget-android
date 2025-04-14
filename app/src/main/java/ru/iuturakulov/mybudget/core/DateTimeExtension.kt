package ru.iuturakulov.mybudget.core

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateTimeExtension {
    fun Long.toIso8601Date(): String {
        val dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
        // Устанавливаем локаль на русский
        // Todo: private val encryptedSharedPreferences: SharedPreferences
        val formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm:ss", Locale("ru"))
        return dateTime.format(formatter)
    }
}