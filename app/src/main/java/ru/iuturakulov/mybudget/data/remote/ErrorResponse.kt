package ru.iuturakulov.mybudget.data.remote

data class ErrorResponse(
    val status: Int,
    val error: String
)