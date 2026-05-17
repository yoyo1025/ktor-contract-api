package com.example.contract.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Instant
import java.time.LocalDate

class ContractTest : DescribeSpec({
    val now = Instant.parse("2025-04-01T00:00:00Z")
    val today = LocalDate.of(2025, 4, 1)

    describe("Contract.create") {
        it("正常に契約を作成できる") {
            val contract =
                Contract.create(
                    title = "業務委託基本契約書",
                    counterparty = "株式会社サンプル",
                    startDate = LocalDate.of(2025, 4, 1),
                    endDate = LocalDate.of(2026, 3, 31),
                    autoRenewal = true,
                    status = ContractStatus.ACTIVE,
                    now = now,
                    today = today,
                )
            contract.title shouldBe "業務委託基本契約書"
            contract.counterparty shouldBe "株式会社サンプル"
            contract.status shouldBe ContractStatus.ACTIVE
            contract.createdAt shouldBe now
            contract.updatedAt shouldBe now
        }

        it("endDate が null でも作成できる") {
            val contract =
                Contract.create(
                    title = "期限なし契約",
                    counterparty = "テスト社",
                    startDate = LocalDate.of(2025, 4, 1),
                    endDate = null,
                    autoRenewal = false,
                    status = ContractStatus.ACTIVE,
                    now = now,
                    today = today,
                )
            contract.endDate shouldBe null
            contract.status shouldBe ContractStatus.ACTIVE
        }

        it("endDate が過去かつ autoRenewal=false の場合、EXPIRED になる") {
            val contract =
                Contract.create(
                    title = "期限切れ契約",
                    counterparty = "テスト社",
                    startDate = LocalDate.of(2024, 1, 1),
                    endDate = LocalDate.of(2025, 3, 31),
                    autoRenewal = false,
                    status = ContractStatus.ACTIVE,
                    now = now,
                    today = today,
                )
            contract.status shouldBe ContractStatus.EXPIRED
        }

        it("endDate が過去でも autoRenewal=true の場合、指定ステータスを維持する") {
            val contract =
                Contract.create(
                    title = "自動更新契約",
                    counterparty = "テスト社",
                    startDate = LocalDate.of(2024, 1, 1),
                    endDate = LocalDate.of(2025, 3, 31),
                    autoRenewal = true,
                    status = ContractStatus.ACTIVE,
                    now = now,
                    today = today,
                )
            contract.status shouldBe ContractStatus.ACTIVE
        }
    }

    describe("Contract バリデーション") {
        it("title が空文字の場合は例外") {
            shouldThrow<IllegalArgumentException> {
                Contract.create(
                    title = "",
                    counterparty = "テスト社",
                    startDate = LocalDate.of(2025, 4, 1),
                    endDate = null,
                    autoRenewal = false,
                    status = ContractStatus.ACTIVE,
                    now = now,
                    today = today,
                )
            }.message shouldBe "title must not be blank"
        }

        it("counterparty が空白のみの場合は例外") {
            shouldThrow<IllegalArgumentException> {
                Contract.create(
                    title = "契約書",
                    counterparty = "   ",
                    startDate = LocalDate.of(2025, 4, 1),
                    endDate = null,
                    autoRenewal = false,
                    status = ContractStatus.ACTIVE,
                    now = now,
                    today = today,
                )
            }.message shouldBe "counterparty must not be blank"
        }

        it("endDate が startDate より前の場合は例外") {
            shouldThrow<IllegalArgumentException> {
                Contract.create(
                    title = "契約書",
                    counterparty = "テスト社",
                    startDate = LocalDate.of(2025, 4, 1),
                    endDate = LocalDate.of(2025, 3, 31),
                    autoRenewal = false,
                    status = ContractStatus.ACTIVE,
                    now = now,
                    today = today,
                )
            }.message shouldBe "endDate must be after startDate"
        }

        it("endDate が startDate と同日の場合は例外") {
            shouldThrow<IllegalArgumentException> {
                Contract.create(
                    title = "契約書",
                    counterparty = "テスト社",
                    startDate = LocalDate.of(2025, 4, 1),
                    endDate = LocalDate.of(2025, 4, 1),
                    autoRenewal = false,
                    status = ContractStatus.ACTIVE,
                    now = now,
                    today = today,
                )
            }.message shouldBe "endDate must be after startDate"
        }
    }

    describe("Contract.resolveStatus") {
        it("endDate が過去かつ autoRenewal=false なら EXPIRED") {
            Contract.resolveStatus(
                status = ContractStatus.ACTIVE,
                endDate = LocalDate.of(2025, 3, 31),
                autoRenewal = false,
                today = today,
            ) shouldBe ContractStatus.EXPIRED
        }

        it("endDate が過去でも autoRenewal=true なら元のステータス") {
            Contract.resolveStatus(
                status = ContractStatus.ACTIVE,
                endDate = LocalDate.of(2025, 3, 31),
                autoRenewal = true,
                today = today,
            ) shouldBe ContractStatus.ACTIVE
        }

        it("endDate が未来なら元のステータス") {
            Contract.resolveStatus(
                status = ContractStatus.ACTIVE,
                endDate = LocalDate.of(2026, 3, 31),
                autoRenewal = false,
                today = today,
            ) shouldBe ContractStatus.ACTIVE
        }

        it("endDate が null なら元のステータス") {
            Contract.resolveStatus(
                status = ContractStatus.ACTIVE,
                endDate = null,
                autoRenewal = false,
                today = today,
            ) shouldBe ContractStatus.ACTIVE
        }

        it("endDate が今日と同日なら EXPIRED にならない") {
            Contract.resolveStatus(
                status = ContractStatus.ACTIVE,
                endDate = today,
                autoRenewal = false,
                today = today,
            ) shouldBe ContractStatus.ACTIVE
        }
    }
})
