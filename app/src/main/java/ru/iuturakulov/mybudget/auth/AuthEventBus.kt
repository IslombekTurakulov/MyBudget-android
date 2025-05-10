package ru.iuturakulov.mybudget.auth

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthEventBus @Inject constructor() {
    private val _unauthorized = MutableSharedFlow<Boolean>(replay = 1)
    val unauthorized: SharedFlow<Boolean> = _unauthorized.asSharedFlow()

    fun publishUnauthorized(value: Boolean) {
        _unauthorized.tryEmit(value)
    }
}
