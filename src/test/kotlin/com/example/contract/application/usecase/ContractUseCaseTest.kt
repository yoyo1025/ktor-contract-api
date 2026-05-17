package com.example.contract.application.usecase

import com.example.contract.domain.model.Contract
import com.example.contract.domain.model.ContractId
import com.example.contract.domain.model.ContractStatus
import com.example.contract.domain.repository.ContractRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.time.Instant
import java.time.LocalDate

class ContractUseCaseTest : DescribeSpec({
    val contractRepository = mockk<ContractRepository>()
    val useCase = ContractUseCase(contractRepository)

    fun createTestContract(
        id: ContractId = ContractId.generate(),
        title: String = "テスト契約",
        counterparty: String = "株式会社テスト",
        status: ContractStatus = ContractStatus.ACTIVE,
        startDate: LocalDate = LocalDate.of(2025, 4, 1),
        endDate: LocalDate? = LocalDate.of(2026, 3, 31),
        autoRenewal: Boolean = true,
    ): Contract {
        val now = Instant.parse("2025-03-15T10:00:00Z")
        return Contract(
            id = id,
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

    describe("create") {
        it("should create a contract and save it") {
            val contractSlot = slot<Contract>()
            every { contractRepository.save(capture(contractSlot)) } answers { contractSlot.captured }

            val command =
                CreateContractCommand(
                    title = "新規契約",
                    counterparty = "株式会社サンプル",
                    startDate = LocalDate.of(2025, 4, 1),
                    endDate = LocalDate.of(2026, 3, 31),
                    autoRenewal = true,
                    status = ContractStatus.ACTIVE,
                )

            val result = useCase.create(command)

            result.title shouldBe "新規契約"
            result.counterparty shouldBe "株式会社サンプル"
            result.status shouldBe ContractStatus.ACTIVE
            verify(exactly = 1) { contractRepository.save(any()) }
        }

        it("should resolve status to EXPIRED when endDate is past and autoRenewal is false") {
            val contractSlot = slot<Contract>()
            every { contractRepository.save(capture(contractSlot)) } answers { contractSlot.captured }

            val command =
                CreateContractCommand(
                    title = "期限切れ契約",
                    counterparty = "株式会社テスト",
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2020, 12, 31),
                    autoRenewal = false,
                    status = ContractStatus.ACTIVE,
                )

            val result = useCase.create(command)

            result.status shouldBe ContractStatus.EXPIRED
        }
    }

    describe("getById") {
        it("should return a contract when found") {
            val contractId = ContractId.generate()
            val contract = createTestContract(id = contractId)
            every { contractRepository.findById(contractId) } returns contract

            val result = useCase.getById(contractId)

            result shouldBe contract
        }

        it("should throw ContractNotFoundException when not found") {
            val contractId = ContractId.generate()
            every { contractRepository.findById(contractId) } returns null

            shouldThrow<ContractNotFoundException> {
                useCase.getById(contractId)
            }
        }
    }

    describe("list") {
        it("should return contracts with total count") {
            val contracts = listOf(createTestContract())
            every {
                contractRepository.findAll(
                    status = ContractStatus.ACTIVE,
                    counterparty = "テスト",
                    limit = 10,
                    offset = 0,
                )
            } returns contracts
            every {
                contractRepository.count(
                    status = ContractStatus.ACTIVE,
                    counterparty = "テスト",
                )
            } returns 1L

            val query =
                ContractListQuery(
                    status = ContractStatus.ACTIVE,
                    counterparty = "テスト",
                    limit = 10,
                    offset = 0,
                )
            val result = useCase.list(query)

            result.contracts shouldBe contracts
            result.total shouldBe 1L
        }
    }

    describe("update") {
        it("should update specified fields only") {
            val contractId = ContractId.generate()
            val existing = createTestContract(id = contractId, title = "旧タイトル")
            every { contractRepository.findById(contractId) } returns existing
            val contractSlot = slot<Contract>()
            every { contractRepository.update(capture(contractSlot)) } answers { contractSlot.captured }

            val command =
                UpdateContractCommand(
                    title = "新タイトル",
                    counterparty = null,
                    startDate = null,
                    endDate = null,
                    autoRenewal = null,
                    status = null,
                )

            val result = useCase.update(contractId, command)

            result.title shouldBe "新タイトル"
            result.counterparty shouldBe "株式会社テスト"
            result.status shouldBe ContractStatus.ACTIVE
        }

        it("should throw ContractNotFoundException when contract does not exist") {
            val contractId = ContractId.generate()
            every { contractRepository.findById(contractId) } returns null

            shouldThrow<ContractNotFoundException> {
                useCase.update(
                    contractId,
                    UpdateContractCommand(
                        title = "更新",
                        counterparty = null,
                        startDate = null,
                        endDate = null,
                        autoRenewal = null,
                        status = null,
                    ),
                )
            }
        }

        it("should resolve status on update") {
            val contractId = ContractId.generate()
            val existing =
                createTestContract(
                    id = contractId,
                    startDate = LocalDate.of(2020, 1, 1),
                    endDate = LocalDate.of(2020, 12, 31),
                    autoRenewal = true,
                )
            every { contractRepository.findById(contractId) } returns existing
            val contractSlot = slot<Contract>()
            every { contractRepository.update(capture(contractSlot)) } answers { contractSlot.captured }

            val command =
                UpdateContractCommand(
                    title = null,
                    counterparty = null,
                    startDate = null,
                    endDate = null,
                    autoRenewal = false,
                    status = ContractStatus.ACTIVE,
                )

            val result = useCase.update(contractId, command)

            result.status shouldBe ContractStatus.EXPIRED
        }
    }

    describe("delete") {
        it("should delete an existing contract") {
            val contractId = ContractId.generate()
            val contract = createTestContract(id = contractId)
            every { contractRepository.findById(contractId) } returns contract
            every { contractRepository.deleteById(contractId) } returns Unit

            useCase.delete(contractId)

            verify(exactly = 1) { contractRepository.deleteById(contractId) }
        }

        it("should throw ContractNotFoundException when contract does not exist") {
            val contractId = ContractId.generate()
            every { contractRepository.findById(contractId) } returns null

            shouldThrow<ContractNotFoundException> {
                useCase.delete(contractId)
            }
        }
    }
})
