package com.example.contract.application.usecase

import com.example.contract.domain.model.Contract
import com.example.contract.domain.model.ContractId
import com.example.contract.domain.model.ContractStatus
import com.example.contract.domain.repository.ContractRepository
import io.github.oshai.kotlinlogging.KotlinLogging
import java.time.Instant
import java.time.LocalDate

private val logger = KotlinLogging.logger {}

data class CreateContractCommand(
    val title: String,
    val counterparty: String,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val autoRenewal: Boolean,
    val status: ContractStatus,
)

data class UpdateContractCommand(
    val title: String?,
    val counterparty: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val autoRenewal: Boolean?,
    val status: ContractStatus?,
)

data class ContractListQuery(
    val status: ContractStatus? = null,
    val counterparty: String? = null,
    val limit: Int = 50,
    val offset: Int = 0,
)

data class ContractListResult(
    val contracts: List<Contract>,
    val total: Long,
)

class ContractUseCase(
    private val contractRepository: ContractRepository,
) {
    fun create(command: CreateContractCommand): Contract {
        val contract =
            Contract.create(
                title = command.title,
                counterparty = command.counterparty,
                startDate = command.startDate,
                endDate = command.endDate,
                autoRenewal = command.autoRenewal,
                status = command.status,
            )
        val saved = contractRepository.save(contract)
        logger.info { "Contract created: ${saved.id.value}" }
        return saved
    }

    fun getById(id: ContractId): Contract {
        return contractRepository.findById(id)
            ?: throw ContractNotFoundException(id)
    }

    fun list(query: ContractListQuery): ContractListResult {
        val contracts =
            contractRepository.findAll(
                status = query.status,
                counterparty = query.counterparty,
                limit = query.limit,
                offset = query.offset,
            )
        val total =
            contractRepository.count(
                status = query.status,
                counterparty = query.counterparty,
            )
        return ContractListResult(contracts = contracts, total = total)
    }

    fun update(
        id: ContractId,
        command: UpdateContractCommand,
    ): Contract {
        val existing =
            contractRepository.findById(id)
                ?: throw ContractNotFoundException(id)

        val now = Instant.now()
        val today = LocalDate.now()

        val newTitle = command.title ?: existing.title
        val newCounterparty = command.counterparty ?: existing.counterparty
        val newStartDate = command.startDate ?: existing.startDate
        val newEndDate = command.endDate ?: existing.endDate
        val newAutoRenewal = command.autoRenewal ?: existing.autoRenewal
        val newStatus = command.status ?: existing.status

        val resolvedStatus = Contract.resolveStatus(newStatus, newEndDate, newAutoRenewal, today)

        val updated =
            existing.copy(
                title = newTitle,
                counterparty = newCounterparty,
                startDate = newStartDate,
                endDate = newEndDate,
                autoRenewal = newAutoRenewal,
                status = resolvedStatus,
                updatedAt = now,
            )
        val result = contractRepository.update(updated)
        logger.info { "Contract updated: ${id.value}" }
        return result
    }

    fun delete(id: ContractId) {
        contractRepository.findById(id)
            ?: throw ContractNotFoundException(id)
        contractRepository.deleteById(id)
        logger.info { "Contract deleted: ${id.value}" }
    }
}

class ContractNotFoundException(val contractId: ContractId) :
    RuntimeException("Contract not found: ${contractId.value}")
