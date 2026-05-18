package com.example.contract.presentation

import com.example.contract.application.usecase.ContractListQuery
import com.example.contract.application.usecase.ContractListResult
import com.example.contract.application.usecase.ContractNotFoundException
import com.example.contract.application.usecase.ContractUseCase
import com.example.contract.application.usecase.CreateContractCommand
import com.example.contract.configureSerialization
import com.example.contract.domain.model.Contract
import com.example.contract.domain.model.ContractId
import com.example.contract.domain.model.ContractStatus
import com.example.contract.presentation.error.configureExceptionHandler
import com.example.contract.presentation.routing.contractRoutes
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.patch
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
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

private fun Application.contractTestModule(contractUseCase: ContractUseCase) {
    configureSerialization()
    configureExceptionHandler()
    routing { route("/api/v1") { contractRoutes(contractUseCase) } }
}

class ContractRouteTest : DescribeSpec({
    val contractUseCase = mockk<ContractUseCase>()

    val sampleContract =
        Contract(
            id = ContractId(UUID.fromString("550e8400-e29b-41d4-a716-446655440000")),
            title = "テスト契約",
            counterparty = "株式会社テスト",
            startDate = LocalDate.of(2025, 4, 1),
            endDate = LocalDate.of(2026, 3, 31),
            autoRenewal = true,
            status = ContractStatus.ACTIVE,
            createdAt = Instant.parse("2025-03-15T10:00:00Z"),
            updatedAt = Instant.parse("2025-03-15T10:00:00Z"),
        )

    describe("GET /api/v1/contracts") {
        it("契約一覧を返す") {
            every { contractUseCase.list(any()) } returns
                ContractListResult(contracts = listOf(sampleContract), total = 1)

            testApplication {
                application { contractTestModule(contractUseCase) }
                val response = client.get("/api/v1/contracts")
                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["total"]?.jsonPrimitive?.content shouldBe "1"
                body["contracts"]?.jsonArray?.size shouldBe 1
            }
        }

        it("クエリパラメータでフィルタリングできる") {
            every {
                contractUseCase.list(
                    ContractListQuery(status = ContractStatus.ACTIVE, limit = 10, offset = 5),
                )
            } returns ContractListResult(contracts = emptyList(), total = 0)

            testApplication {
                application { contractTestModule(contractUseCase) }
                val response = client.get("/api/v1/contracts?status=ACTIVE&limit=10&offset=5")
                response.status shouldBe HttpStatusCode.OK
                verify {
                    contractUseCase.list(
                        ContractListQuery(status = ContractStatus.ACTIVE, limit = 10, offset = 5),
                    )
                }
            }
        }
    }

    describe("POST /api/v1/contracts") {
        it("契約を作成して201を返す") {
            every { contractUseCase.create(any<CreateContractCommand>()) } returns sampleContract

            testApplication {
                application { contractTestModule(contractUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }
                val response =
                    client.post("/api/v1/contracts") {
                        contentType(ContentType.Application.Json)
                        setBody(
                            """
                            {
                                "title":"テスト契約",
                                "counterparty":"株式会社テスト",
                                "startDate":"2025-04-01",
                                "endDate":"2026-03-31",
                                "autoRenewal":true,
                                "status":"ACTIVE"
                            }
                            """.trimIndent(),
                        )
                    }
                response.status shouldBe HttpStatusCode.Created
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["id"]?.jsonPrimitive?.content shouldBe "550e8400-e29b-41d4-a716-446655440000"
                body["title"]?.jsonPrimitive?.content shouldBe "テスト契約"
            }
        }
    }

    describe("GET /api/v1/contracts/{id}") {
        it("存在する契約を返す") {
            every { contractUseCase.getById(any()) } returns sampleContract

            testApplication {
                application { contractTestModule(contractUseCase) }
                val response = client.get("/api/v1/contracts/550e8400-e29b-41d4-a716-446655440000")
                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["title"]?.jsonPrimitive?.content shouldBe "テスト契約"
            }
        }

        it("存在しない契約は404を返す") {
            val id = ContractId(UUID.fromString("00000000-0000-0000-0000-000000000099"))
            every { contractUseCase.getById(id) } throws ContractNotFoundException(id)

            testApplication {
                application { contractTestModule(contractUseCase) }
                val response = client.get("/api/v1/contracts/00000000-0000-0000-0000-000000000099")
                response.status shouldBe HttpStatusCode.NotFound
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "NOT_FOUND"
            }
        }

        it("無効なUUIDは400を返す") {
            testApplication {
                application { contractTestModule(contractUseCase) }
                val response = client.get("/api/v1/contracts/invalid-uuid")
                response.status shouldBe HttpStatusCode.BadRequest
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "VALIDATION_ERROR"
            }
        }
    }

    describe("PATCH /api/v1/contracts/{id}") {
        it("契約を更新して200を返す") {
            val updated = sampleContract.copy(title = "更新後の契約")
            every { contractUseCase.update(any(), any()) } returns updated

            testApplication {
                application { contractTestModule(contractUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }
                val response =
                    client.patch("/api/v1/contracts/550e8400-e29b-41d4-a716-446655440000") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"更新後の契約"}""")
                    }
                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["title"]?.jsonPrimitive?.content shouldBe "更新後の契約"
            }
        }
    }

    describe("DELETE /api/v1/contracts/{id}") {
        it("契約を削除して204を返す") {
            justRun { contractUseCase.delete(any()) }

            testApplication {
                application { contractTestModule(contractUseCase) }
                val response =
                    client.delete("/api/v1/contracts/550e8400-e29b-41d4-a716-446655440000")
                response.status shouldBe HttpStatusCode.NoContent
            }
        }
    }

    describe("不正リクエストのエラーハンドリング") {
        it("不正なJSONは400を返す") {
            testApplication {
                application { contractTestModule(contractUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }
                val response =
                    client.post("/api/v1/contracts") {
                        contentType(ContentType.Application.Json)
                        setBody("""{ invalid json """)
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "VALIDATION_ERROR"
            }
        }

        it("必須フィールド欠落は400を返す") {
            testApplication {
                application { contractTestModule(contractUseCase) }
                val client = createClient { install(ContentNegotiation) { json() } }
                val response =
                    client.post("/api/v1/contracts") {
                        contentType(ContentType.Application.Json)
                        setBody("""{"title":"テスト"}""")
                    }
                response.status shouldBe HttpStatusCode.BadRequest
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["error"]?.jsonObject?.get("code")?.jsonPrimitive?.content shouldBe "VALIDATION_ERROR"
            }
        }
    }
})
