package com.example.contract.domain.model

import java.util.UUID

@JvmInline
value class UserId(val value: UUID) {
    companion object {
        fun generate(): UserId = UserId(UUID.randomUUID())

        fun fromString(value: String): UserId = UserId(UUID.fromString(value))
    }
}
