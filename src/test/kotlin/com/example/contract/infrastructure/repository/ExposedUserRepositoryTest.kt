package com.example.contract.infrastructure.repository

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer

class ExposedUserRepositoryTest : DescribeSpec({
    val postgres = PostgreSQLContainer("postgres:15-alpine").apply {
        withDatabaseName("contract_test")
        withUsername("testuser")
        withPassword("testpass")
    }

    lateinit var repository: ExposedUserRepository

    beforeSpec {
        postgres.start()
        Database.connect(
            url = postgres.jdbcUrl,
            driver = "org.postgresql.Driver",
            user = postgres.username,
            password = postgres.password,
        )
        Flyway.configure()
            .dataSource(postgres.jdbcUrl, postgres.username, postgres.password)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        repository = ExposedUserRepository()
    }

    afterSpec {
        postgres.stop()
    }

    describe("findByLoginId") {
        it("should find the initial admin user") {
            val user = repository.findByLoginId("admin")

            user shouldNotBe null
            user!!.loginId shouldBe "admin"
            user.name shouldBe "Administrator"
        }

        it("should return null for non-existent loginId") {
            val user = repository.findByLoginId("nonexistent")

            user shouldBe null
        }
    }
})
