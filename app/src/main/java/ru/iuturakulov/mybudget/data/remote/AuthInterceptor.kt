package ru.iuturakulov.mybudget.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import ru.iuturakulov.mybudget.auth.TokenStorage
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val tokenStorage: TokenStorage
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val accessToken = tokenStorage.getAccessToken()

        return if (!accessToken.isNullOrEmpty()) {
            chain.proceed(
                request.newBuilder()
                    .header("Authorization", "Bearer $accessToken")
                    .build()
            )
        } else {
            chain.proceed(request)
        }
    }
}