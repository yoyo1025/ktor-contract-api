package com.example.contract.presentation.error

import com.example.contract.application.usecase.AuthenticationException
import com.example.contract.application.usecase.ContractNotFoundException
import com.example.contract.presentation.dto.ErrorBody
import com.example.contract.presentation.dto.ErrorDetail
import com.example.contract.presentation.dto.ErrorResponse
import com.example.contract.presentation.dto.InvalidRequestException
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond

fun Application.configureExceptionHandler() {
    install(StatusPages) {
        exception<InvalidRequestException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error =
                        ErrorBody(
                            code = "VALIDATION_ERROR",
                            message = "${cause.field} ${cause.reason}",
                            details = listOf(ErrorDetail(field = cause.field, reason = cause.reason)),
                        ),
                ),
            )
        }
        exception<IllegalArgumentException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(
                    error =
                        ErrorBody(
                            code = "VALIDATION_ERROR",
                            message = cause.message ?: "Invalid request",
                        ),
                ),
            )
        }
        exception<AuthenticationException> { call, cause ->
            call.respond(
                HttpStatusCode.Unauthorized,
                ErrorResponse(
                    error =
                        ErrorBody(
                            code = "UNAUTHORIZED",
                            message = cause.message ?: "Authentication failed",
                        ),
                ),
            )
        }
        exception<ContractNotFoundException> { call, _ ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(
                    error =
                        ErrorBody(
                            code = "NOT_FOUND",
                            message = "Contract not found",
                        ),
                ),
            )
        }
        exception<Exception> { call, _ ->
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(
                    error =
                        ErrorBody(
                            code = "INTERNAL_ERROR",
                            message = "An unexpected error occurred",
                        ),
                ),
            )
        }
    }
}
