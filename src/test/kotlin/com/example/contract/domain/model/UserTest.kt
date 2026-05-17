package com.example.contract.domain.model

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

class UserTest : DescribeSpec({
    describe("User") {
        it("正常にユーザを生成できる") {
            val user =
                User(
                    id = UserId.generate(),
                    loginId = "admin",
                    passwordHash = "\$2a\$10\$dummyhash",
                    name = "Administrator",
                    createdAt = Instant.parse("2025-04-01T00:00:00Z"),
                )
            user.loginId shouldBe "admin"
            user.name shouldBe "Administrator"
        }

        it("loginId が空文字の場合は例外") {
            shouldThrow<IllegalArgumentException> {
                User(
                    id = UserId.generate(),
                    loginId = "",
                    passwordHash = "\$2a\$10\$dummyhash",
                    name = "Administrator",
                    createdAt = Instant.now(),
                )
            }.message shouldBe "loginId must not be blank"
        }

        it("loginId が100文字を超える場合は例外") {
            shouldThrow<IllegalArgumentException> {
                User(
                    id = UserId.generate(),
                    loginId = "a".repeat(101),
                    passwordHash = "\$2a\$10\$dummyhash",
                    name = "Administrator",
                    createdAt = Instant.now(),
                )
            }.message shouldBe "loginId must be at most 100 characters"
        }

        it("name が空白のみの場合は例外") {
            shouldThrow<IllegalArgumentException> {
                User(
                    id = UserId.generate(),
                    loginId = "admin",
                    passwordHash = "\$2a\$10\$dummyhash",
                    name = "   ",
                    createdAt = Instant.now(),
                )
            }.message shouldBe "name must not be blank"
        }

        it("name が255文字を超える場合は例外") {
            shouldThrow<IllegalArgumentException> {
                User(
                    id = UserId.generate(),
                    loginId = "admin",
                    passwordHash = "\$2a\$10\$dummyhash",
                    name = "a".repeat(256),
                    createdAt = Instant.now(),
                )
            }.message shouldBe "name must be at most 255 characters"
        }
    }
})
