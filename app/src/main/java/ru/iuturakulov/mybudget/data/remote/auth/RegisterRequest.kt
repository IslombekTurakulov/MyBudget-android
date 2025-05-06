package ru.iuturakulov.mybudget.data.remote.auth

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val code: String,
)