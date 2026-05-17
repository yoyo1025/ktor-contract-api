package com.example.contract.application.usecase

import com.example.contract.domain.model.Contract
import com.example.contract.domain.model.ContractId
import com.example.contract.domain.model.ContractStatus
import com.example.contract.domain.repository.ContractRepository
import java.time.Instant
import java.time.LocalDate

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
        return contractRepository.save(contract)
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
        return contractRepository.update(updated)
    }

    fun delete(id: ContractId) {
        contractRepository.findById(id)
            ?: throw ContractNotFoundException(id)
        contractRepository.deleteById(id)
    }
}

class ContractNotFoundException(val contractId: ContractId) :
    RuntimeException("Contract not found: ${contractId.value}")
