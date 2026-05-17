package com.example.contract

import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    embeddedServer(Netty, port = 8080) {
        module()
    }.start(wait = true)
}

fun io.ktor.server.application.Application.module() {
    configureSerialization()
    configureRouting()
}
