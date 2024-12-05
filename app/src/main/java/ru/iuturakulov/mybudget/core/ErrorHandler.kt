package ru.iuturakulov.mybudget.core

object ErrorHandler {
    fun getErrorMessage(exception: Throwable): String {
        return when (exception) {
            is java.net.UnknownHostException -> "Нет подключения к интернету"
            is java.net.SocketTimeoutException -> "Время ожидания истекло"
            else -> exception.message ?: "Произошла неизвестная ошибка"
        }
    }
}