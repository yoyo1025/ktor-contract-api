package com.example.contract.presentation

import com.example.contract.module
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class HealthRouteTest : DescribeSpec({
    describe("GET /health") {
        it("200 OK と status=UP, database=UP を返す") {
            testApplication {
                application { module() }
                val response = client.get("/health")
                response.status shouldBe HttpStatusCode.OK
                val body = Json.parseToJsonElement(response.bodyAsText()).jsonObject
                body["status"]?.jsonPrimitive?.content shouldBe "UP"
                body["database"]?.jsonPrimitive?.content shouldBe "UP"
            }
        }
    }
})
