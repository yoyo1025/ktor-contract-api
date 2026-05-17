package com.example.contract.infrastructure.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.timestamp

object ContractsTable : Table("contracts") {
    val id = uuid("id")
    val title = varchar("title", 255)
    val counterparty = varchar("counterparty", 255)
    val startDate = date("start_date")
    val endDate = date("end_date").nullable()
    val autoRenewal = bool("auto_renewal")
    val status = varchar("status", 20)
    val createdAt = timestamp("created_at")
    val updatedAt = timestamp("updated_at")

    override val primaryKey = PrimaryKey(id)
}
