package ru.iuturakulov.mybudget.data.remote.auth

data class TokensResponse(
    val accessToken: String,
    val refreshToken: String
)