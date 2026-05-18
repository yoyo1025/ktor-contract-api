package com.example.contract.presentation

import com.example.contract.application.port.TokenResult
import com.example.contract.application.usecase.ContractNotFoundException
import com.example.contract.application.usecase.ContractUseCase
import com.example.contract.application.usecase.LoginUseCase
import com.example.contract.config.JwtConfig
import com.example.contract.config.JwtTokenGenerator
import com.example.contract.config.configureSecurity
import com.example.contract.configureRateLimit
import com.example.contract.configureRouting
import com.example.contract.configureSerialization
import com.example.contract.domain.model.ContractId
import com.example.contract.domain.model.UserId
import com.example.contract.presentation.error.configureExceptionHandler
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID

private val testJwtConfig =
    JwtConfig(
        secret = "test-secret-key-for-testing-purposes-only",
        issuer = "test-issuer",
        audience = "test-audience",
        expiresInSeconds = 3600,
    )

private val testTokenGenerator = JwtTokenGenerator(testJwtConfig)

private fun Application.errorTestModule(
    loginUseCase: LoginUseCase,
    contractUseCase: ContractUseCase,
) {
    configureSerialization()
    configureExceptionHandler()
    configureSecurity(testJwtConfig)
    configureRateLimit()
    configureRouting(loginUseCase, contractUseCase)
}

class ErrorHandlingTest : DescribeSpec({
    val loginUseCase = mockk<LoginUseCase>()
    val contractUseCase = mockk<ContractUseCase>()
    val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))

    describe("Rate Limit 429 エラーレスポンス") {
        it("Rate Limit超過時に共通エラーフォーマットでRATE_LIMITEDを返す") {
            every { loginUseCase.execute(any()) } returns
                TokenResult(accessToken = "token", tokenType = "Bearer", expiresIn = 3600)

            testApplication {
                application { errorTestModule(loginUseCase, contractUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }

                repeat(60) {
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"loginId":"admin","password":"pass"}""")
                        header("X-Forwarded-For", "192.168.1.200")
                    }
                }

                val response =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"loginId":"admin","password":"pass"}""")
                        header("X-Forwarded-For", "192.168.1.200")
                    }
                response.status shouldBe HttpStatusCode.TooManyRequests
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "RATE_LIMITED"
                body["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content shouldBe
                    "Too many requests. Please try again later."
            }
        }
    }

    describe("500 Internal Server Error") {
        it("予期せぬ例外は共通エラーフォーマットでINTERNAL_ERRORを返す") {
            every { contractUseCase.list(any()) } throws RuntimeException("Unexpected DB error")
            val token = testTokenGenerator.generate(userId).accessToken

            testApplication {
                application { errorTestModule(loginUseCase, contractUseCase) }
                val response =
                    client.get("/api/v1/contracts") {
                        bearerAuth(token)
                    }
                response.status shouldBe HttpStatusCode.InternalServerError
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "INTERNAL_ERROR"
                body["error"]?.jsonObject?.get("message")?.jsonPrimitive?.content shouldBe
                    "An unexpected error occurred"
            }
        }
    }

    describe("全エラーレスポンスの形式") {
        it("401エラーは共通エラーフォーマットに従う") {
            testApplication {
                application { errorTestModule(loginUseCase, contractUseCase) }
                val response = client.get("/api/v1/contracts")
                response.status shouldBe HttpStatusCode.Unauthorized
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val error = body["error"]?.jsonObject
                error?.get("code")?.jsonPrimitive?.content shouldBe "UNAUTHORIZED"
                error?.get("message")?.jsonPrimitive?.content shouldBe "Token is invalid or expired"
            }
        }

        it("404エラーは共通エラーフォーマットに従う") {
            val notFoundId = ContractId(UUID.fromString("00000000-0000-0000-0000-000000000099"))
            every { contractUseCase.getById(notFoundId) } throws
                ContractNotFoundException(notFoundId)
            val token = testTokenGenerator.generate(userId).accessToken

            testApplication {
                application { errorTestModule(loginUseCase, contractUseCase) }
                val response =
                    client.get("/api/v1/contracts/00000000-0000-0000-0000-000000000099") {
                        bearerAuth(token)
                    }
                response.status shouldBe HttpStatusCode.NotFound
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "NOT_FOUND"
            }
        }

        it("400バリデーションエラーはdetailsを含む") {
            val token = testTokenGenerator.generate(userId).accessToken

            testApplication {
                application { errorTestModule(loginUseCase, contractUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }
                val response =
                    client.post("/api/v1/contracts") {
                        bearerAuth(token)
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "title":"test",
                                "counterparty":"test",
                                "startDate":"invalid-date",
                                "autoRenewal":true,
                                "status":"ACTIVE"
                            }
                            """.trimIndent(),
                        )
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                val error = body["error"]?.jsonObject
                error?.get("code")?.jsonPrimitive?.content shouldBe "VALIDATION_ERROR"
                val details = error?.get("details")
                details shouldBe
                    Json.parseToJsonElement(
                        """[{"field":"startDate","reason":"must be a valid date (YYYY-MM-DD)"}]""",
                    )
            }
        }
    }
})
