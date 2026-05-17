package com.example.contract.domain.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class ContractIdTest : DescribeSpec({
    describe("ContractId") {
        it("generate() はランダムなIDを生成する") {
            val id1 = ContractId.generate()
            val id2 = ContractId.generate()
            id1 shouldNotBe id2
        }

        it("fromString() は文字列からContractIdを生成する") {
            val uuid = "550e8400-e29b-41d4-a716-446655440000"
            val id = ContractId.fromString(uuid)
            id.value shouldBe UUID.fromString(uuid)
        }

        it("同じUUIDを持つContractIdは等価である") {
            val uuid = UUID.randomUUID()
            ContractId(uuid) shouldBe ContractId(uuid)
        }
    }
})
