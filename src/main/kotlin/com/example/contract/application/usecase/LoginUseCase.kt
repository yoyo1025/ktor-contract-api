package com.example.contract.application.usecase

import com.example.contract.application.port.PasswordVerifier
import com.example.contract.application.port.TokenGenerator
import com.example.contract.application.port.TokenResult
import com.example.contract.domain.repository.UserRepository

data class LoginCommand(
    val loginId: String,
    val password: String,
)

class LoginUseCase(
    private val userRepository: UserRepository,
    private val passwordVerifier: PasswordVerifier,
    private val tokenGenerator: TokenGenerator,
) {
    fun execute(command: LoginCommand): TokenResult {
        val user =
            userRepository.findByLoginId(command.loginId)
                ?: throw AuthenticationException("Invalid login credentials")

        if (!passwordVerifier.verify(command.password, user.passwordHash)) {
            throw AuthenticationException("Invalid login credentials")
        }

        return tokenGenerator.generate(user.id)
    }
}

class AuthenticationException(message: String) : RuntimeException(message)
