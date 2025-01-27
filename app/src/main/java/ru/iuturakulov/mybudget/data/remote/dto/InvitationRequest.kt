package ru.iuturakulov.mybudget.data.remote.dto

data class InvitationRequest(
    val projectId: String,
    val email: String,
    val role: String
)