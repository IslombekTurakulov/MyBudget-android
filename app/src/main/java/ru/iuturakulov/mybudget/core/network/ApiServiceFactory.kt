package ru.iuturakulov.mybudget.core.network

import retrofit2.Retrofit

object ApiServiceFactory {
    inline fun <reified T> createService(retrofitBuilder: Retrofit.Builder, baseUrl: String): T {
        return retrofitBuilder.baseUrl(baseUrl).build().create(T::class.java)
    }
}