package com.example.contract.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class ErrorResponse(
    val error: ErrorBody,
)

@Serializable
data class ErrorBody(
    val code: String,
    val message: String,
    val details: List<ErrorDetail>? = null,
)

@Serializable
data class ErrorDetail(
    val field: String,
    val reason: String,
)
