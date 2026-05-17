package com.example.contract.application.usecase

import com.example.contract.application.port.PasswordVerifier
import com.example.contract.application.port.TokenGenerator
import com.example.contract.application.port.TokenResult
import com.example.contract.domain.model.User
import com.example.contract.domain.model.UserId
import com.example.contract.domain.repository.UserRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import java.time.Instant

class LoginUseCaseTest : DescribeSpec({
    val userRepository = mockk<UserRepository>()
    val passwordVerifier = mockk<PasswordVerifier>()
    val tokenGenerator = mockk<TokenGenerator>()

    val useCase = LoginUseCase(userRepository, passwordVerifier, tokenGenerator)

    val testUser =
        User(
            id = UserId.generate(),
            loginId = "admin",
            passwordHash = "\$2a\$10\$hashedpassword",
            name = "Administrator",
            createdAt = Instant.now(),
        )

    describe("execute") {
        it("should return token when credentials are valid") {
            val expectedToken =
                TokenResult(
                    accessToken = "jwt-token",
                    tokenType = "Bearer",
                    expiresIn = 3600,
                )
            every { userRepository.findByLoginId("admin") } returns testUser
            every { passwordVerifier.verify("password123", testUser.passwordHash) } returns true
            every { tokenGenerator.generate(testUser.id) } returns expectedToken

            val result = useCase.execute(LoginCommand("admin", "password123"))

            result.accessToken shouldBe "jwt-token"
            result.tokenType shouldBe "Bearer"
            result.expiresIn shouldBe 3600
        }

        it("should throw AuthenticationException when user not found") {
            every { userRepository.findByLoginId("unknown") } returns null

            val exception =
                shouldThrow<AuthenticationException> {
                    useCase.execute(LoginCommand("unknown", "password123"))
                }
            exception.message shouldBe "Invalid login credentials"
        }

        it("should throw AuthenticationException when password is wrong") {
            every { userRepository.findByLoginId("admin") } returns testUser
            every { passwordVerifier.verify("wrongpassword", testUser.passwordHash) } returns false

            val exception =
                shouldThrow<AuthenticationException> {
                    useCase.execute(LoginCommand("admin", "wrongpassword"))
                }
            exception.message shouldBe "Invalid login credentials"
        }
    }
})
