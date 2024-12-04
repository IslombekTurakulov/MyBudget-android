package ru.iuturakulov.mybudget.core.network

import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

abstract class BaseApiClient<T : Any>(private val apiService: T) {

    /**
     * Выполняет безопасный API-вызов, обрабатывает и логгирует ошибки.
     *
     * @param apiCall Вызов API, который будет выполнен
     * @return ApiResponse с данными или ошибкой
     */
    suspend fun <R> safeApiCall(
        apiCall: suspend T.() -> R,
        defaultErrorMessage: String = "Произошла неизвестная ошибка"
    ): ApiResponse<R> {
        return try {
            val response = apiCall(apiService)
            ApiResponse.Success(response)
        } catch (e: Exception) {
            Timber.e(e, "API call failed")
            ApiResponse.Error(handleException(e, defaultErrorMessage))
        }
    }

    /**
     * Обрабатывает исключение и возвращает понятное сообщение об ошибке.
     *
     * @param exception Исключение, возникшее при выполнении вызова API
     * @param defaultErrorMessage Сообщение по умолчанию, если исключение неизвестное
     * @return Сообщение об ошибке
     */
    private fun handleException(exception: Exception, defaultErrorMessage: String): String {
        return when (exception) {
            is IOException -> "Ошибка сети, пожалуйста, проверьте подключение"
            is HttpException -> {
                val code = exception.code()
                val errorMessage = exception.message()
                "Ошибка сервера (код $code): $errorMessage"
            }
            else -> defaultErrorMessage
        }
    }
}
