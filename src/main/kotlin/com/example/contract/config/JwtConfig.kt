package com.example.contract.config

data class JwtConfig(
    val secret: String,
    val issuer: String,
    val audience: String,
    val expiresInSeconds: Long,
) {
    init {
        require(secret.length >= MIN_SECRET_LENGTH) {
            "JWT secret must be at least $MIN_SECRET_LENGTH characters"
        }
    }

    companion object {
        const val MIN_SECRET_LENGTH = 32
        private const val LOCAL_DEFAULT_SECRET = "dev-secret-key-for-local-development-only"

        fun fromEnvironment(): JwtConfig {
            val appEnv = System.getenv("APP_ENV") ?: "local"
            val secret =
                System.getenv("JWT_SECRET")
                    ?: if (appEnv == "local") {
                        LOCAL_DEFAULT_SECRET
                    } else {
                        error("JWT_SECRET environment variable is required in '$appEnv' environment")
                    }
            return JwtConfig(
                secret = secret,
                issuer = System.getenv("JWT_ISSUER") ?: "ktor-contract-api",
                audience = System.getenv("JWT_AUDIENCE") ?: "ktor-contract-api",
                expiresInSeconds = System.getenv("JWT_EXPIRES_IN_SECONDS")?.toLongOrNull() ?: 3600,
            )
        }
    }
}
