package com.example.contract.config

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.contract.application.port.TokenGenerator
import com.example.contract.application.port.TokenResult
import com.example.contract.domain.model.UserId
import java.util.Date

class JwtTokenGenerator(
    private val jwtConfig: JwtConfig,
) : TokenGenerator {
    override fun generate(userId: UserId): TokenResult {
        val now = System.currentTimeMillis()
        val token =
            JWT.create()
                .withSubject(userId.value.toString())
                .withIssuer(jwtConfig.issuer)
                .withAudience(jwtConfig.audience)
                .withIssuedAt(Date(now))
                .withExpiresAt(Date(now + jwtConfig.expiresInSeconds * 1000))
                .sign(Algorithm.HMAC256(jwtConfig.secret))
        return TokenResult(accessToken = token, expiresIn = jwtConfig.expiresInSeconds)
    }
}
