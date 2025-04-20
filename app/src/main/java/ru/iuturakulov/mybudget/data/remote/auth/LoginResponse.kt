package ru.iuturakulov.mybudget.data.remote.auth

import kotlinx.serialization.Serializable

@Serializable
data class TokenData(
    val accessToken: String,
    val refreshToken: String,
)