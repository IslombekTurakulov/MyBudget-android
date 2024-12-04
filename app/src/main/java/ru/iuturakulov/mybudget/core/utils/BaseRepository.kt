package ru.iuturakulov.mybudget.core.utils

import ru.iuturakulov.mybudget.core.network.ApiResponse

abstract class BaseRepository<T, R>(
    private val dao: T,
    private val apiClient: R
) {

    protected suspend fun <V> fetchFromNetwork(
        localQuery: suspend T.() -> V,
        networkCall: suspend R.() -> ApiResponse<V>,
        saveCallResult: suspend T.(V) -> Unit
    ): ApiResponse<V> {
        return when (val response = networkCall(apiClient)) {
            is ApiResponse.Success -> {
                dao.saveCallResult(response.data)
                ApiResponse.Success(response.data)
            }
            is ApiResponse.Error -> {
                val localData = localQuery(dao)
                if (localData != null) ApiResponse.Success(localData) else response
            }
        }
    }
}
