package com.example.contract.presentation.routing

import com.example.contract.HealthResponse
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.healthRoute() {
    get("/health") {
        call.respond(HealthResponse(status = "UP", database = "UP"))
    }
}
