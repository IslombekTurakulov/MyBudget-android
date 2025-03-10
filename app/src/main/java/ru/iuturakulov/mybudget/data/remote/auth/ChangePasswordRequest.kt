package ru.iuturakulov.mybudget.data.remote.auth

data class ChangePasswordRequest(
    val email: String,
    val oldPassword: String,
    val newPassword: String,
)