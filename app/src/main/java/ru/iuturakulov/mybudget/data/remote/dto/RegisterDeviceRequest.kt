package ru.iuturakulov.mybudget.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(
    val token: String,
    val language: String,
    val platform: String = "android"
)

@Serializable
data class PreferencesRequest(val types: List<String>)

@Serializable
data class InvitationPreferencesResponse(
    val types: List<String>
)