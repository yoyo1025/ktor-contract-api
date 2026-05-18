package com.example.contract.presentation

import com.example.contract.application.port.TokenResult
import com.example.contract.application.usecase.ContractListResult
import com.example.contract.application.usecase.ContractUseCase
import com.example.contract.application.usecase.LoginCommand
import com.example.contract.application.usecase.LoginUseCase
import com.example.contract.config.JwtConfig
import com.example.contract.config.JwtTokenGenerator
import com.example.contract.config.configureSecurity
import com.example.contract.configureRateLimit
import com.example.contract.configureRouting
import com.example.contract.configureSerialization
import com.example.contract.domain.model.UserId
import com.example.contract.presentation.error.configureExceptionHandler
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
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
        secret = "test-secret-key-for-testing",
        issuer = "test-issuer",
        audience = "test-audience",
        expiresInSeconds = 3600,
    )

private val testTokenGenerator = JwtTokenGenerator(testJwtConfig)

private fun Application.securityTestModule(
    loginUseCase: LoginUseCase,
    contractUseCase: ContractUseCase,
) {
    configureSerialization()
    configureExceptionHandler()
    configureSecurity(testJwtConfig)
    configureRateLimit()
    configureRouting(loginUseCase, contractUseCase)
}

class SecurityTest : DescribeSpec({
    val loginUseCase = mockk<LoginUseCase>()
    val contractUseCase = mockk<ContractUseCase>()
    val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))

    describe("JWT認証") {
        it("トークンなしで契約APIにアクセスすると401を返す") {
            testApplication {
                application { securityTestModule(loginUseCase, contractUseCase) }
                val response = client.get("/api/v1/contracts")
                response.status shouldBe HttpStatusCode.Unauthorized
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "UNAUTHORIZED"
            }
        }

        it("無効なトークンで契約APIにアクセスすると401を返す") {
            testApplication {
                application { securityTestModule(loginUseCase, contractUseCase) }
                val response =
                    client.get("/api/v1/contracts") {
                        bearerAuth("invalid-token")
                    }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        it("不正な署名鍵のトークンで契約APIにアクセスすると401を返す") {
            val wrongConfig = testJwtConfig.copy(secret = "wrong-secret")
            val wrongGenerator = JwtTokenGenerator(wrongConfig)
            val wrongToken = wrongGenerator.generate(userId).accessToken

            testApplication {
                application { securityTestModule(loginUseCase, contractUseCase) }
                val response =
                    client.get("/api/v1/contracts") {
                        bearerAuth(wrongToken)
                    }
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }

        it("有効なトークンで契約APIにアクセスできる") {
            val token = testTokenGenerator.generate(userId).accessToken
            every { contractUseCase.list(any()) } returns
                ContractListResult(contracts = emptyList(), total = 0)

            testApplication {
                application { securityTestModule(loginUseCase, contractUseCase) }
                val response =
                    client.get("/api/v1/contracts") {
                        bearerAuth(token)
                    }
                response.status shouldBe HttpStatusCode.OK
            }
        }

        it("ログインAPIは認証不要でアクセスできる") {
            val tokenResult = testTokenGenerator.generate(userId)
            every { loginUseCase.execute(LoginCommand("admin", "password123")) } returns
                TokenResult(
                    accessToken = tokenResult.accessToken,
                    tokenType = "Bearer",
                    expiresIn = 3600,
                )

            testApplication {
                application { securityTestModule(loginUseCase, contractUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }
                val response =
                    client.post("/api/v1/auth/login") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"loginId":"admin","password":"password123"}""")
                    }
                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["accessToken"]?.jsonPrimitive?.content shouldBe tokenResult.accessToken
            }
        }

        it("ヘルスチェックは認証不要でアクセスできる") {
            testApplication {
                application { securityTestModule(loginUseCase, contractUseCase) }
                val response = client.get("/health")
                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})
