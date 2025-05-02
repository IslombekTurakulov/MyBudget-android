package ru.iuturakulov.mybudget.data.remote.dto

data class InvitationRequest(
    val projectId: String,
    val email: String?,
    val type: InvitationType = InvitationType.MANUAL,
    val role: String
) {
    enum class InvitationType {
        QR,
        MANUAL;
    }
}