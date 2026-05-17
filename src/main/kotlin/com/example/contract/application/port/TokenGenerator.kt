package com.example.contract.application.port

import com.example.contract.domain.model.UserId

data class TokenResult(
    val accessToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
)

interface TokenGenerator {
    fun generate(userId: UserId): TokenResult
}
