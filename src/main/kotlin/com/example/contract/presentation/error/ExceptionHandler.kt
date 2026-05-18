package com.example.contract.presentation.error

import com.example.contract.application.usecase.AuthenticationException
import com.example.contract.application.usecase.ContractNotFoundException
import com.example.contract.presentation.dto.ErrorBody
import com.example.contract.presentation.dto.ErrorDetail
import com.example.contract.presentation.dto.ErrorResponse
import com.example.contract.presentation.dto.InvalidRequestException
import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.ContentTransformationException
import io.ktor.server.response.respond

private val logger = KotlinLogging.logger {}

fun Application.configureExceptionHandler() {
    install(StatusPages) {
        status(HttpStatusCode.TooManyRequests) { call, status ->
            logger.warn { "Rate limit exceeded" }
            call.respond(status, simpleError("RATE_LIMITED", "Too many requests. Please try again later."))
        }
        exception<InvalidRequestException> { call, cause ->
            logger.debug { "Validation error: ${cause.field} ${cause.reason}" }
            call.respond(HttpStatusCode.BadRequest, validationError(cause.field, cause.reason))
        }
        exception<IllegalArgumentException> { call, cause ->
            logger.debug { "Invalid argument: ${cause.message}" }
            call.respond(HttpStatusCode.BadRequest, simpleError("VALIDATION_ERROR", cause.message ?: "Invalid request"))
        }
        exception<BadRequestException> { call, cause ->
            logger.debug { "Bad request: ${cause.message}" }
            call.respond(HttpStatusCode.BadRequest, simpleError("VALIDATION_ERROR", cause.message ?: "Bad request"))
        }
        exception<ContentTransformationException> { call, cause ->
            logger.debug { "Content transformation error: ${cause.message}" }
            call.respond(
                HttpStatusCode.BadRequest,
                simpleError("VALIDATION_ERROR", cause.message ?: "Invalid request body"),
            )
        }
        exception<AuthenticationException> { call, cause ->
            logger.warn { "Authentication failed: ${cause.message}" }
            call.respond(HttpStatusCode.Unauthorized, simpleError("UNAUTHORIZED", cause.message ?: "Authentication failed"))
        }
        exception<ContractNotFoundException> { call, cause ->
            logger.debug { "Contract not found: ${cause.contractId.value}" }
            call.respond(HttpStatusCode.NotFound, simpleError("NOT_FOUND", "Contract not found"))
        }
        exception<Exception> { call, cause ->
            logger.error(cause) { "Unexpected error" }
            call.respond(HttpStatusCode.InternalServerError, simpleError("INTERNAL_ERROR", "An unexpected error occurred"))
        }
    }
}

private fun validationError(
    field: String,
    reason: String,
): ErrorResponse =
    ErrorResponse(
        error =
            ErrorBody(
                code = "VALIDATION_ERROR",
                message = "$field $reason",
                details = listOf(ErrorDetail(field = field, reason = reason)),
            ),
    )

private fun simpleError(
    code: String,
    message: String,
): ErrorResponse = ErrorResponse(error = ErrorBody(code = code, message = message))
