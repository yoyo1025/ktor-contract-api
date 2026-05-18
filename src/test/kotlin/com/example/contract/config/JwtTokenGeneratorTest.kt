package com.example.contract.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.contract.domain.model.UserId
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.util.UUID

class JwtTokenGeneratorTest : DescribeSpec({
    val jwtConfig =
        JwtConfig(
            secret = "test-secret-key-for-testing-purposes",
            issuer = "test-issuer",
            audience = "test-audience",
            expiresInSeconds = 3600,
        )
    val generator = JwtTokenGenerator(jwtConfig)

    describe("generate") {
        it("有効なJWTトークンを生成する") {
            val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
            val result = generator.generate(userId)

            result.tokenType shouldBe "Bearer"
            result.expiresIn shouldBe 3600
            result.accessToken shouldNotBe ""

            val verifier =
                JWT.require(Algorithm.HMAC256("test-secret-key-for-testing-purposes"))
                    .withIssuer("test-issuer")
                    .withAudience("test-audience")
                    .build()
            val decoded = verifier.verify(result.accessToken)
            decoded.subject shouldBe userId.value.toString()
            decoded.issuer shouldBe "test-issuer"
            decoded.audience shouldBe listOf("test-audience")
        }
    }
})
