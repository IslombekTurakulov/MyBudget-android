package ru.iuturakulov.mybudget.data.remote.auth

data class LoginRequest(
    val email: String,
    val password: String
)