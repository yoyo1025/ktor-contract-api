package com.example.contract.presentation.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val loginId: String,
    val password: String,
)

@Serializable
data class LoginResponse(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Long,
)
