package ru.iuturakulov.mybudget.utils

import java.util.regex.Pattern

object UrlValidator {
    private val URL_PATTERN = Pattern.compile(
        "^" +
            // протокол (опционально)
            "(?:(?:https?|ftp)://)?" +
            // домен
            "(?:" +
                // IP адрес
                "(?:(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}(?:[0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])" +
                "|" +
                // localhost
                "localhost" +
                "|" +
                // доменное имя
                "(?:(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)" +
                // поддомены
                "(?:\\.(?:[a-z\\u00a1-\\uffff0-9]-*)*[a-z\\u00a1-\\uffff0-9]+)*" +
                // TLD
                "(?:\\.(?:[a-z\\u00a1-\\uffff]{2,}))" +
            ")" +
            // порт (опционально)
            "(?::\\d{2,5})?" +
            // путь (опционально)
            "(?:/\\S*)?" +
        "$",
        Pattern.CASE_INSENSITIVE
    )

    fun isValidUrl(url: String): Boolean {
        return URL_PATTERN.matcher(url.trim()).matches()
    }
} 