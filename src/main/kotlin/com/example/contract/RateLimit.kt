package com.example.contract

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.ratelimit.RateLimitName
import kotlin.time.Duration.Companion.minutes

val LOGIN_RATE_LIMIT = RateLimitName("login")

fun Application.configureRateLimit() {
    install(RateLimit) {
        register(LOGIN_RATE_LIMIT) {
            rateLimiter(limit = 60, refillPeriod = 1.minutes)
        }
    }
}
