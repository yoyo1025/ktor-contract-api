package com.example.contract

import com.example.contract.application.usecase.ContractUseCase
import com.example.contract.application.usecase.LoginUseCase
import com.example.contract.presentation.routing.authRoutes
import com.example.contract.presentation.routing.contractRoutes
import com.example.contract.presentation.routing.healthRoute
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.routing.route
import io.ktor.server.routing.routing

fun Application.configureRouting(
    loginUseCase: LoginUseCase,
    contractUseCase: ContractUseCase,
) {
    routing {
        healthRoute()
        route("/api/v1") {
            rateLimit(LOGIN_RATE_LIMIT) {
                authRoutes(loginUseCase)
            }
            authenticate("auth-jwt") {
                contractRoutes(contractUseCase)
            }
        }
    }
}
