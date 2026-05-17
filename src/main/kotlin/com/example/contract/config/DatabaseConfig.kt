package com.example.contract.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import java.sql.Connection
import javax.sql.DataSource

object DatabaseConfig {
    fun setup(): DataSource {
        val dataSource = createDataSource()
        runMigrations(dataSource)
        updateAdminPasswordHash(dataSource)
        Database.connect(dataSource)
        return dataSource
    }

    fun createDataSource(): HikariDataSource {
        val config =
            HikariConfig().apply {
                jdbcUrl = buildJdbcUrl()
                username = System.getenv("DB_USER") ?: "appuser"
                password = System.getenv("DB_PASSWORD") ?: "apppassword"
                driverClassName = "org.postgresql.Driver"
                maximumPoolSize = 10
                isAutoCommit = false
                transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            }
        return HikariDataSource(config)
    }

    fun runMigrations(dataSource: DataSource) {
        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
    }

    private fun updateAdminPasswordHash(dataSource: DataSource) {
        val hash = System.getenv("ADMIN_PASSWORD_HASH") ?: return
        dataSource.connection.use { conn: Connection ->
            conn.prepareStatement(
                "UPDATE users SET password_hash = ? WHERE login_id = 'admin'",
            ).use { stmt ->
                stmt.setString(1, hash)
                stmt.executeUpdate()
            }
            conn.commit()
        }
    }

    private fun buildJdbcUrl(): String {
        val host = System.getenv("DB_HOST") ?: "localhost"
        val port = System.getenv("DB_PORT") ?: "5432"
        val name = System.getenv("DB_NAME") ?: "contract_one"
        return "jdbc:postgresql://$host:$port/$name"
    }
}
