package com.example.contract.config

import com.example.contract.configureSerialization
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.slf4j.MDC

@Serializable
data class MdcSnapshot(
    val requestId: String?,
    val method: String?,
    val path: String?,
)

class RequestLoggingTest : DescribeSpec({
    describe("RequestLogging") {
        it("リクエスト処理中にMDCにrequestId, method, pathが設定される") {
            testApplication {
                application {
                    configureRequestLogging()
                    configureSerialization()
                    routing {
                        get("/test-mdc") {
                            val snapshot =
                                MdcSnapshot(
                                    requestId = MDC.get("requestId"),
                                    method = MDC.get("method"),
                                    path = MDC.get("path"),
                                )
                            call.respond(snapshot)
                        }
                    }
                }
                val response = client.get("/test-mdc")
                response.status shouldBe HttpStatusCode.OK
                val body = Json.decodeFromString<MdcSnapshot>(response.bodyAsText())
                body.requestId shouldNotBe null
                body.requestId!!.length shouldBe 36
                body.method shouldBe "GET"
                body.path shouldBe "/test-mdc"
            }
        }

        it("各リクエストで異なるrequestIdが生成される") {
            testApplication {
                application {
                    configureRequestLogging()
                    configureSerialization()
                    routing {
                        get("/test-mdc") {
                            call.respond(
                                MdcSnapshot(
                                    requestId = MDC.get("requestId"),
                                    method = MDC.get("method"),
                                    path = MDC.get("path"),
                                ),
                            )
                        }
                    }
                }
                val response1 = client.get("/test-mdc")
                val response2 = client.get("/test-mdc")
                val body1 = Json.decodeFromString<MdcSnapshot>(response1.bodyAsText())
                val body2 = Json.decodeFromString<MdcSnapshot>(response2.bodyAsText())
                body1.requestId shouldNotBe body2.requestId
            }
        }
    }
})
