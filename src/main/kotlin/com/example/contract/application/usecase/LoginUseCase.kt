package com.example.contract.application.usecase

import com.example.contract.application.port.PasswordVerifier
import com.example.contract.application.port.TokenGenerator
import com.example.contract.application.port.TokenResult
import com.example.contract.domain.repository.UserRepository
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

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
                ?: run {
                    logger.warn { "Login failed: user not found for loginId=${command.loginId}" }
                    throw AuthenticationException("Invalid login credentials")
                }

        if (!passwordVerifier.verify(command.password, user.passwordHash)) {
            logger.warn { "Login failed: invalid password for loginId=${command.loginId}" }
            throw AuthenticationException("Invalid login credentials")
        }

        logger.info { "Login successful: userId=${user.id.value}" }
        return tokenGenerator.generate(user.id)
    }
}

class AuthenticationException(message: String) : RuntimeException(message)
