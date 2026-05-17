package com.example.contract.domain.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe

class ContractStatusTest : DescribeSpec({
    describe("ContractStatus") {
        it("3つの状態を持つ") {
            ContractStatus.entries.map { it.name } shouldBe listOf("ACTIVE", "EXPIRED", "CANCELLED")
        }

        it("valueOf で文字列から変換できる") {
            ContractStatus.valueOf("ACTIVE") shouldBe ContractStatus.ACTIVE
            ContractStatus.valueOf("EXPIRED") shouldBe ContractStatus.EXPIRED
            ContractStatus.valueOf("CANCELLED") shouldBe ContractStatus.CANCELLED
        }
    }
})
