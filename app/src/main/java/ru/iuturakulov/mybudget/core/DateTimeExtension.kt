package ru.iuturakulov.mybudget.core

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeExtension {
    fun Long.toIso8601Date(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault())
        return formatter.format(Instant.ofEpochMilli(this))
    }
}