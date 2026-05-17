package com.example.contract.domain.model

import java.time.Instant

data class User(
    val id: UserId,
    val loginId: String,
    val passwordHash: String,
    val name: String,
    val createdAt: Instant,
) {
    init {
        require(loginId.isNotBlank()) { "loginId must not be blank" }
        require(loginId.length <= MAX_LOGIN_ID_LENGTH) { "loginId must be at most $MAX_LOGIN_ID_LENGTH characters" }
        require(name.isNotBlank()) { "name must not be blank" }
        require(name.length <= MAX_NAME_LENGTH) { "name must be at most $MAX_NAME_LENGTH characters" }
    }

    companion object {
        private const val MAX_LOGIN_ID_LENGTH = 100
        private const val MAX_NAME_LENGTH = 255
    }
}
