package com.example.contract.domain.model

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class UserIdTest : DescribeSpec({
    describe("UserId") {
        it("generate() はランダムなIDを生成する") {
            val id1 = UserId.generate()
            val id2 = UserId.generate()
            id1 shouldNotBe id2
        }

        it("fromString() は文字列からUserIdを生成する") {
            val uuid = "00000000-0000-0000-0000-000000000001"
            val id = UserId.fromString(uuid)
            id.value shouldBe UUID.fromString(uuid)
        }

        it("同じUUIDを持つUserIdは等価である") {
            val uuid = UUID.randomUUID()
            UserId(uuid) shouldBe UserId(uuid)
        }
    }
})
