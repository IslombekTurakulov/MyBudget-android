package ru.iuturakulov.mybudget.data.remote.auth

data class VerifyEmailRequest(
    val email: String,
    val code: String
)
