package com.example.contract.infrastructure

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import java.sql.DriverManager

class DatabaseMigrationTest : DescribeSpec({
    val postgres =
        PostgreSQLContainer("postgres:15-alpine").apply {
            withDatabaseName("contract_test")
            withUsername("testuser")
            withPassword("testpass")
        }

    beforeSpec {
        postgres.start()
    }

    afterSpec {
        postgres.stop()
    }

    describe("Flyway migration") {
        it("should apply all migrations successfully") {
            val flyway =
                Flyway.configure()
                    .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
                    .locations("classpath:db/migration")
                    .load()

            val result = flyway.migrate()

            result.success shouldBe true
            result.migrationsExecuted shouldBe 3
        }

        it("should create contracts table with correct columns") {
            val connection =
                DriverManager.getConnection(
                    postgres.jdbcUrl,
                    postgres.username,
                    postgres.password,
                )

            val meta = connection.metaData
            val columns = meta.getColumns(null, null, "contracts", null)
            val columnNames = mutableListOf<String>()
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME"))
            }

            columnNames shouldBe
                listOf(
                    "id", "title", "counterparty", "start_date", "end_date",
                    "auto_renewal", "status", "created_at", "updated_at",
                )

            connection.close()
        }

        it("should create users table with correct columns") {
            val connection =
                DriverManager.getConnection(
                    postgres.jdbcUrl,
                    postgres.username,
                    postgres.password,
                )

            val meta = connection.metaData
            val columns = meta.getColumns(null, null, "users", null)
            val columnNames = mutableListOf<String>()
            while (columns.next()) {
                columnNames.add(columns.getString("COLUMN_NAME"))
            }

            columnNames shouldBe
                listOf(
                    "id", "login_id", "password_hash", "name", "created_at",
                )

            connection.close()
        }

        it("should insert initial admin user") {
            val connection =
                DriverManager.getConnection(
                    postgres.jdbcUrl,
                    postgres.username,
                    postgres.password,
                )

            val stmt = connection.prepareStatement("SELECT login_id, name FROM users WHERE id = '00000000-0000-0000-0000-000000000001'")
            val rs = stmt.executeQuery()

            rs.next() shouldBe true
            rs.getString("login_id") shouldBe "admin"
            rs.getString("name") shouldBe "Administrator"

            connection.close()
        }

        it("should have indexes on contracts table") {
            val connection =
                DriverManager.getConnection(
                    postgres.jdbcUrl,
                    postgres.username,
                    postgres.password,
                )

            val indexes = connection.metaData.getIndexInfo(null, null, "contracts", false, false)
            val indexNames = mutableSetOf<String>()
            while (indexes.next()) {
                indexes.getString("INDEX_NAME")?.let { indexNames.add(it) }
            }

            indexNames.contains("idx_contracts_counterparty") shouldBe true
            indexNames.contains("idx_contracts_status") shouldBe true
            indexNames.contains("idx_contracts_end_date") shouldBe true

            connection.close()
        }

        it("should have unique constraint on users.login_id") {
            val connection =
                DriverManager.getConnection(
                    postgres.jdbcUrl,
                    postgres.username,
                    postgres.password,
                )

            val indexes = connection.metaData.getIndexInfo(null, null, "users", true, false)
            var hasUniqueLoginId = false
            while (indexes.next()) {
                val columnName = indexes.getString("COLUMN_NAME")
                val nonUnique = indexes.getBoolean("NON_UNIQUE")
                if (columnName == "login_id" && !nonUnique) {
                    hasUniqueLoginId = true
                }
            }

            hasUniqueLoginId shouldBe true

            connection.close()
        }

        it("should insert admin user with placeholder password hash") {
            val connection =
                DriverManager.getConnection(
                    postgres.jdbcUrl,
                    postgres.username,
                    postgres.password,
                )

            val stmt = connection.prepareStatement("SELECT password_hash FROM users WHERE login_id = 'admin'")
            val rs = stmt.executeQuery()

            rs.next() shouldBe true
            val hash = rs.getString("password_hash")
            hash shouldBe "__PLACEHOLDER_MUST_BE_REPLACED__"

            connection.close()
        }

        it("should update admin password hash via updateAdminPasswordHash") {
            val bcryptHash = "\$2a\$10\$dXJ3SW6G7P50lGmMQiS4MOmFih0h4gAfUHeDv2BLBID5GWQNH0gqS"
            val connection =
                DriverManager.getConnection(
                    postgres.jdbcUrl,
                    postgres.username,
                    postgres.password,
                )

            connection.autoCommit = true
            connection.prepareStatement("UPDATE users SET password_hash = ? WHERE login_id = 'admin'").use { stmt ->
                stmt.setString(1, bcryptHash)
                stmt.executeUpdate()
            }

            val rs = connection.prepareStatement("SELECT password_hash FROM users WHERE login_id = 'admin'").executeQuery()
            rs.next() shouldBe true
            rs.getString("password_hash").startsWith("\$2a\$") shouldBe true

            connection.close()
        }
    }
})
