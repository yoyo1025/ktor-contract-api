package com.example.contract

import com.example.contract.config.DatabaseConfig
import com.zaxxer.hikari.HikariDataSource
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

fun main() {
    val port = System.getenv("PORT")?.toIntOrNull() ?: 8080
    embeddedServer(Netty, port = port) {
        module()
    }.start(wait = true)
}

fun Application.module() {
    val dataSource = DatabaseConfig.setup()
    monitor.subscribe(ApplicationStopped) {
        (dataSource as? HikariDataSource)?.close()
    }
    configureSerialization()
    configureRouting()
}

