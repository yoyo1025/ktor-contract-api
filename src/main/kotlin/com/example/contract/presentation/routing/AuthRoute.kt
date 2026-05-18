package com.example.contract.presentation.routing

import com.example.contract.application.usecase.LoginCommand
import com.example.contract.application.usecase.LoginUseCase
import com.example.contract.presentation.dto.LoginRequest
import com.example.contract.presentation.dto.LoginResponse
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.authRoutes(loginUseCase: LoginUseCase) {
    route("/auth") {
        post("/login") {
            val request = call.receive<LoginRequest>()
            val result =
                loginUseCase.execute(
                    LoginCommand(
                        loginId = request.loginId,
                        password = request.password,
                    ),
                )
            call.respond(
                LoginResponse(
                    accessToken = result.accessToken,
                    tokenType = result.tokenType,
                    expiresIn = result.expiresIn,
                ),
            )
        }
    }
}
