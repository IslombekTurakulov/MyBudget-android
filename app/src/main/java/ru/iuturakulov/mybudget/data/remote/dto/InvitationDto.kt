package ru.iuturakulov.mybudget.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvitationDto(
    val message: String,
    val qrCodeBase64: String?,
    val inviteCode: String?,
)
