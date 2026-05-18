package com.example.contract.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import org.slf4j.MDC
import java.util.UUID

private val logger = KotlinLogging.logger {}

fun Application.configureRequestLogging() {
    intercept(ApplicationCallPipeline.Setup) {
        val requestId = UUID.randomUUID().toString()
        val method = call.request.httpMethod.value
        val path = call.request.path()

        MDC.put("requestId", requestId)
        MDC.put("method", method)
        MDC.put("path", path)

        try {
            logger.info { "Request started: $method $path" }
            proceed()
            val status = call.response.status()?.value ?: 0
            logger.info { "Request completed: $method $path -> $status" }
        } finally {
            MDC.clear()
        }
    }
}
