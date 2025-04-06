package ru.iuturakulov.mybudget.core

import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

object NetworkErrorHandler {
    fun getErrorMessage(throwable: Throwable): String {
        Timber.e(throwable, "Ошибка выполнения запроса")
        return when (throwable) {
            is IOException -> "Проблема с подключением к сети. Проверьте соединение."
            is HttpException -> {
                when (throwable.code()) {
                    400 -> "Некорректный запрос."
                    401 -> "Неверные учетные данные."
                    403 -> "Доступ запрещен."
                    404 -> "Ресурс не найден."
                    500 -> "Ошибка на стороне сервера."
                    else -> "Неизвестная ошибка: ${throwable.code()}."
                }
            }

            else -> "${throwable.localizedMessage}."
        }
    }
}