package com.example.contract.config

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import kotlinx.coroutines.slf4j.MDCContext
import kotlinx.coroutines.withContext
import org.slf4j.MDC
import java.util.UUID

private val logger = KotlinLogging.logger {}

private val MDC_KEYS = listOf("requestId", "method", "path")

fun Application.configureRequestLogging() {
    intercept(ApplicationCallPipeline.Setup) {
        val requestId = UUID.randomUUID().toString()
        val method = call.request.httpMethod.value
        val path = call.request.path()

        val oldValues = MDC_KEYS.associateWith { MDC.get(it) }

        MDC.put("requestId", requestId)
        MDC.put("method", method)
        MDC.put("path", path)

        try {
            withContext(MDCContext()) {
                logger.info { "Request started: $method $path" }
                proceed()
                val status = call.response.status()?.value ?: 0
                logger.info { "Request completed: $method $path -> $status" }
            }
        } finally {
            oldValues.forEach { (key, value) ->
                if (value != null) {
                    MDC.put(key, value)
                } else {
                    MDC.remove(key)
                }
            }
        }
    }
}
