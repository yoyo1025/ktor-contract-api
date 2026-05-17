package com.example.contract.domain.model

import java.util.UUID

@JvmInline
value class ContractId(val value: UUID) {
    companion object {
        fun generate(): ContractId = ContractId(UUID.randomUUID())

        fun fromString(value: String): ContractId = ContractId(UUID.fromString(value))
    }
}
