package com.example.contract.infrastructure.repository

import com.example.contract.domain.model.Contract
import com.example.contract.domain.model.ContractId
import com.example.contract.domain.model.ContractStatus
import com.example.contract.domain.repository.ContractRepository
import com.example.contract.infrastructure.table.ContractsTable
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.andWhere
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update

class ExposedContractRepository : ContractRepository {

    override fun findById(id: ContractId): Contract? = transaction {
        ContractsTable.selectAll()
            .where { ContractsTable.id eq id.value }
            .map { it.toContract() }
            .singleOrNull()
    }

    override fun findAll(
        status: ContractStatus?,
        counterparty: String?,
        limit: Int,
        offset: Int,
    ): List<Contract> = transaction {
        val query = ContractsTable.selectAll()
        if (status != null) {
            query.andWhere { ContractsTable.status eq status.name }
        }
        if (counterparty != null) {
            query.andWhere { ContractsTable.counterparty like "%$counterparty%" }
        }
        query.limit(limit).offset(offset.toLong())
            .map { it.toContract() }
    }

    override fun count(
        status: ContractStatus?,
        counterparty: String?,
    ): Long = transaction {
        val query = ContractsTable.selectAll()
        if (status != null) {
            query.andWhere { ContractsTable.status eq status.name }
        }
        if (counterparty != null) {
            query.andWhere { ContractsTable.counterparty like "%$counterparty%" }
        }
        query.count()
    }

    override fun save(contract: Contract): Contract = transaction {
        ContractsTable.insert {
            it[id] = contract.id.value
            it[title] = contract.title
            it[counterparty] = contract.counterparty
            it[startDate] = contract.startDate
            it[endDate] = contract.endDate
            it[autoRenewal] = contract.autoRenewal
            it[status] = contract.status.name
            it[createdAt] = contract.createdAt
            it[updatedAt] = contract.updatedAt
        }
        contract
    }

    override fun update(contract: Contract): Contract = transaction {
        ContractsTable.update({ ContractsTable.id eq contract.id.value }) {
            it[title] = contract.title
            it[counterparty] = contract.counterparty
            it[startDate] = contract.startDate
            it[endDate] = contract.endDate
            it[autoRenewal] = contract.autoRenewal
            it[status] = contract.status.name
            it[updatedAt] = contract.updatedAt
        }
        contract
    }

    override fun deleteById(id: ContractId): Unit = transaction {
        ContractsTable.deleteWhere { ContractsTable.id eq id.value }
    }

    private fun ResultRow.toContract(): Contract = Contract(
        id = ContractId(this[ContractsTable.id]),
        title = this[ContractsTable.title],
        counterparty = this[ContractsTable.counterparty],
        startDate = this[ContractsTable.startDate],
        endDate = this[ContractsTable.endDate],
        autoRenewal = this[ContractsTable.autoRenewal],
        status = ContractStatus.valueOf(this[ContractsTable.status]),
        createdAt = this[ContractsTable.createdAt],
        updatedAt = this[ContractsTable.updatedAt],
    )
}
