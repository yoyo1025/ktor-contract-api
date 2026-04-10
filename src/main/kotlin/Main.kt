package org.example

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable

@Serializable
data class Message(val text: String)

fun main() {
    embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            json()
        }
        routing {
            get("/") {
                call.respondText("Hello, Ktor!")
            }
            post("/echo") {
                val message = call.receive<Message>()
                call.respond(message)
            }
        }
    }.start(wait = true)
}
