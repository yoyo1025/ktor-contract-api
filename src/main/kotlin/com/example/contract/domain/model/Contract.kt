package com.example.contract.domain.model

import java.time.Instant
import java.time.LocalDate

data class Contract(
    val id: ContractId,
    val title: String,
    val counterparty: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val autoRenewal: Boolean,
    val status: ContractStatus,
    val createdAt: Instant,
    val updatedAt: Instant,
) {
    init {
        require(title.isNotBlank()) { "title must not be blank" }
        require(title.length <= MAX_TITLE_LENGTH) { "title must be at most $MAX_TITLE_LENGTH characters" }
        require(counterparty.isNotBlank()) { "counterparty must not be blank" }
        require(counterparty.length <= MAX_COUNTERPARTY_LENGTH) {
            "counterparty must be at most $MAX_COUNTERPARTY_LENGTH characters"
        }
        if (endDate != null) {
            require(endDate.isAfter(startDate)) { "endDate must be after startDate" }
        }
    }

    companion object {
        private const val MAX_TITLE_LENGTH = 255
        private const val MAX_COUNTERPARTY_LENGTH = 255

        fun create(
            title: String,
            counterparty: String,
            startDate: LocalDate,
            endDate: LocalDate?,
            autoRenewal: Boolean,
            status: ContractStatus,
            now: Instant = Instant.now(),
            today: LocalDate = LocalDate.now(),
        ): Contract {
            val resolvedStatus = resolveStatus(status, endDate, autoRenewal, today)
            return Contract(
                id = ContractId.generate(),
                title = title,
                counterparty = counterparty,
                startDate = startDate,
                endDate = endDate,
                autoRenewal = autoRenewal,
                status = resolvedStatus,
                createdAt = now,
                updatedAt = now,
            )
        }

        fun resolveStatus(
            status: ContractStatus,
            endDate: LocalDate?,
            autoRenewal: Boolean,
            today: LocalDate = LocalDate.now(),
        ): ContractStatus =
            if (endDate != null && endDate.isBefore(today) && !autoRenewal) {
                ContractStatus.EXPIRED
            } else {
                status
            }
    }
}
