package com.example.contract.infrastructure.table

import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.timestamp

object UsersTable : Table("users") {
    val id = uuid("id")
    val loginId = varchar("login_id", 100)
    val passwordHash = varchar("password_hash", 255)
    val name = varchar("name", 255)
    val createdAt = timestamp("created_at")

    override val primaryKey = PrimaryKey(id)
}
