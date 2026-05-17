package com.example.contract.infrastructure.repository

import com.example.contract.domain.model.Contract
import com.example.contract.domain.model.ContractId
import com.example.contract.domain.model.ContractStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.testcontainers.containers.PostgreSQLContainer
import java.time.Instant
import java.time.LocalDate

class ExposedContractRepositoryTest : DescribeSpec({
    val postgres = PostgreSQLContainer("postgres:15-alpine").apply {
        withDatabaseName("contract_test")
        withUsername("testuser")
        withPassword("testpass")
    }

    lateinit var repository: ExposedContractRepository

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
        repository = ExposedContractRepository()
    }

    afterSpec {
        postgres.stop()
    }

    fun createTestContract(
        title: String = "テスト契約",
        counterparty: String = "株式会社テスト",
        status: ContractStatus = ContractStatus.ACTIVE,
        startDate: LocalDate = LocalDate.of(2025, 4, 1),
        endDate: LocalDate? = LocalDate.of(2026, 3, 31),
        autoRenewal: Boolean = true,
    ): Contract {
        val now = Instant.now()
        return Contract(
            id = ContractId.generate(),
            title = title,
            counterparty = counterparty,
            startDate = startDate,
            endDate = endDate,
            autoRenewal = autoRenewal,
            status = status,
            createdAt = now,
            updatedAt = now,
        )
    }

    describe("save") {
        it("should save and retrieve a contract") {
            val contract = createTestContract()
            repository.save(contract)

            val found = repository.findById(contract.id)

            found shouldNotBe null
            found!!.id shouldBe contract.id
            found.title shouldBe "テスト契約"
            found.counterparty shouldBe "株式会社テスト"
            found.status shouldBe ContractStatus.ACTIVE
            found.autoRenewal shouldBe true
        }
    }

    describe("findById") {
        it("should return null for non-existent id") {
            val found = repository.findById(ContractId.generate())
            found shouldBe null
        }
    }

    describe("findAll") {
        it("should filter by status") {
            val active = createTestContract(title = "有効契約A", status = ContractStatus.ACTIVE)
            val cancelled = createTestContract(title = "解約済みA", status = ContractStatus.CANCELLED)
            repository.save(active)
            repository.save(cancelled)

            val results = repository.findAll(status = ContractStatus.CANCELLED)

            results.any { it.id == cancelled.id } shouldBe true
            results.none { it.id == active.id } shouldBe true
        }

        it("should filter by counterparty partial match") {
            val contract = createTestContract(title = "部分一致テスト", counterparty = "ユニークカンパニー")
            repository.save(contract)

            val results = repository.findAll(counterparty = "ユニーク")

            results.any { it.id == contract.id } shouldBe true
        }

        it("should support limit and offset") {
            val contracts = (1..5).map {
                createTestContract(title = "ページング契約$it", counterparty = "ページング社")
            }
            contracts.forEach { repository.save(it) }

            val page = repository.findAll(counterparty = "ページング社", limit = 2, offset = 0)

            page shouldHaveSize 2
        }
    }

    describe("count") {
        it("should count contracts with filters") {
            val contract = createTestContract(title = "カウント用", counterparty = "カウント社", status = ContractStatus.EXPIRED)
            repository.save(contract)

            val total = repository.count(status = ContractStatus.EXPIRED, counterparty = "カウント社")

            total shouldBe 1L
        }
    }

    describe("update") {
        it("should update an existing contract") {
            val contract = createTestContract(title = "更新前")
            repository.save(contract)

            val updated = contract.copy(title = "更新後", updatedAt = Instant.now())
            repository.update(updated)

            val found = repository.findById(contract.id)
            found shouldNotBe null
            found!!.title shouldBe "更新後"
        }
    }

    describe("deleteById") {
        it("should delete an existing contract") {
            val contract = createTestContract(title = "削除対象")
            repository.save(contract)

            repository.deleteById(contract.id)

            val found = repository.findById(contract.id)
            found shouldBe null
        }
    }
})
