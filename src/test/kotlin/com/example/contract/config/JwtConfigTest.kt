package com.example.contract.config

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.string.shouldContain

class JwtConfigTest : DescribeSpec({
    describe("JwtConfig") {
        it("secretが32文字以上であれば生成できる") {
            JwtConfig(
                secret = "a".repeat(32),
                issuer = "test",
                audience = "test",
                expiresInSeconds = 3600,
            )
        }

        it("secretが32文字未満の場合はエラーになる") {
            val exception =
                shouldThrow<IllegalArgumentException> {
                    JwtConfig(
                        secret = "short-secret",
                        issuer = "test",
                        audience = "test",
                        expiresInSeconds = 3600,
                    )
                }
            exception.message shouldContain "at least 32 characters"
        }
    }
})
