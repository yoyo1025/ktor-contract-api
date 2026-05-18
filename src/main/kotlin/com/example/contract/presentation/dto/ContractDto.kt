package com.example.contract.presentation.dto

import com.example.contract.domain.model.Contract
import com.example.contract.domain.model.ContractStatus
import kotlinx.serialization.Serializable
import java.time.format.DateTimeFormatter

@Serializable
data class CreateContractRequest(
    val title: String,
    val counterparty: String,
    val startDate: String,
    val endDate: String? = null,
    val autoRenewal: Boolean,
    val status: String,
)

@Serializable
data class UpdateContractRequest(
    val title: String? = null,
    val counterparty: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val autoRenewal: Boolean? = null,
    val status: String? = null,
)

@Serializable
data class ContractResponse(
    val id: String,
    val title: String,
    val counterparty: String,
    val startDate: String,
    val endDate: String?,
    val autoRenewal: Boolean,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ContractListResponse(
    val contracts: List<ContractResponse>,
    val total: Long,
)

fun Contract.toResponse(): ContractResponse {
    val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    val instantFormatter = DateTimeFormatter.ISO_INSTANT
    return ContractResponse(
        id = id.value.toString(),
        title = title,
        counterparty = counterparty,
        startDate = startDate.format(dateFormatter),
        endDate = endDate?.format(dateFormatter),
        autoRenewal = autoRenewal,
        status = status.name,
        createdAt = instantFormatter.format(createdAt),
        updatedAt = instantFormatter.format(updatedAt),
    )
}

fun String.toContractStatus(): ContractStatus =
    try {
        ContractStatus.valueOf(this.uppercase())
    } catch (_: IllegalArgumentException) {
        throw InvalidRequestException("status", "must be one of: ACTIVE, EXPIRED, CANCELLED")
    }

class InvalidRequestException(
    val field: String,
    val reason: String,
) : RuntimeException("$field $reason")
