package com.example.contract.presentation

import com.example.contract.application.port.TokenResult
import com.example.contract.application.usecase.AuthenticationException
import com.example.contract.application.usecase.LoginCommand
import com.example.contract.application.usecase.LoginUseCase
import com.example.contract.configureSerialization
import com.example.contract.presentation.error.configureExceptionHandler
import com.example.contract.presentation.routing.authRoutes
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private fun Application.authTestModule(loginUseCase: LoginUseCase) {
    configureSerialization()
    configureExceptionHandler()
    routing { route("/api/v1") { authRoutes(loginUseCase) } }
}

class AuthRouteTest : DescribeSpec({
    val loginUseCase = mockk<LoginUseCase>()

    describe("POST /api/v1/auth/login") {
        it("正しい認証情報でログインするとトークンを返す") {
            every { loginUseCase.execute(LoginCommand("admin", "password123")) } returns
                TokenResult(
                    accessToken = "test-token",
                    tokenType = "Bearer",
                    expiresIn = 3600,
                )

            testApplication {
                application { authTestModule(loginUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }
                val response =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"loginId":"admin","password":"password123"}""")
                    }
                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["accessToken"]?.jsonPrimitive?.content shouldBe "test-token"
                body["tokenType"]?.jsonPrimitive?.content shouldBe "Bearer"
                body["expiresIn"]?.jsonPrimitive?.content shouldBe "3600"
            }
        }

        it("認証失敗時に401を返す") {
            every { loginUseCase.execute(any()) } throws
                AuthenticationException("Invalid login credentials")

            testApplication {
                application { authTestModule(loginUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }
                val response =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"loginId":"admin","password":"wrong"}""")
                    }
                response.status shouldBe HttpStatusCode.Unauthorized
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "UNAUTHORIZED"
            }
        }

        it("不正なJSONは400を返す") {
            testApplication {
                application { authTestModule(loginUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }
                val response =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{ not valid json""")
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "VALIDATION_ERROR"
            }
        }
    }
})
