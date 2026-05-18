package com.example.contract.config

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expiresInSeconds: Long,
) {
    companion object {
        fun fromEnvironment(): JwtConfig =
            JwtConfig(
                secret = System.getenv("JWT_SECRET") ?: "dev-secret-key-for-local-development-only",
                issuer = System.getenv("JWT_ISSUER") ?: "ktor-contract-api",
                audience = System.getenv("JWT_AUDIENCE") ?: "ktor-contract-api",
                expiresInSeconds = System.getenv("JWT_EXPIRES_IN_SECONDS")?.toLongOrNull() ?: 3600,
            )
    }
}
