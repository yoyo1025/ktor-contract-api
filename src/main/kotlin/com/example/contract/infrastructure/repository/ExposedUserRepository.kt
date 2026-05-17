package com.example.contract.infrastructure.repository

import com.example.contract.domain.model.User
import com.example.contract.domain.model.UserId
import com.example.contract.domain.repository.UserRepository
import com.example.contract.infrastructure.table.UsersTable
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.transactions.transaction

class ExposedUserRepository : UserRepository {

    override fun findByLoginId(loginId: String): User? = transaction {
        UsersTable.selectAll()
            .where { UsersTable.loginId eq loginId }
            .map { row ->
                User(
                    id = UserId(row[UsersTable.id]),
                    loginId = row[UsersTable.loginId],
                    passwordHash = row[UsersTable.passwordHash],
                    name = row[UsersTable.name],
                    createdAt = row[UsersTable.createdAt],
                )
            }
            .singleOrNull()
    }
}
