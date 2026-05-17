package com.example.contract.domain.repository

import com.example.contract.domain.model.Contract
import com.example.contract.domain.model.ContractId
import com.example.contract.domain.model.ContractStatus

interface ContractRepository {
    fun findById(id: ContractId): Contract?

    fun findAll(
        status: ContractStatus? = null,
        counterparty: String? = null,
        limit: Int = 50,
        offset: Int = 0,
    ): List<Contract>

    fun count(
        status: ContractStatus? = null,
        counterparty: String? = null,
    ): Long

    fun save(contract: Contract): Contract

    fun update(contract: Contract): Contract

    fun deleteById(id: ContractId)
}
