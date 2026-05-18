package com.example.contract.presentation.routing

import com.example.contract.application.usecase.ContractListQuery
import com.example.contract.application.usecase.ContractUseCase
import com.example.contract.application.usecase.CreateContractCommand
import com.example.contract.application.usecase.UpdateContractCommand
import com.example.contract.domain.model.ContractId
import com.example.contract.domain.model.ContractStatus
import com.example.contract.presentation.dto.ContractListResponse
import com.example.contract.presentation.dto.CreateContractRequest
import com.example.contract.presentation.dto.InvalidRequestException
import com.example.contract.presentation.dto.UpdateContractRequest
import com.example.contract.presentation.dto.toContractStatus
import com.example.contract.presentation.dto.toResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.patch
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import java.time.LocalDate
import java.time.format.DateTimeParseException

fun Route.contractRoutes(contractUseCase: ContractUseCase) {
    route("/contracts") {
        listContracts(contractUseCase)
        createContract(contractUseCase)
        getContract(contractUseCase)
        updateContract(contractUseCase)
        deleteContract(contractUseCase)
    }
}

private fun Route.listContracts(contractUseCase: ContractUseCase) {
    get {
        val status = call.request.queryParameters["status"]?.let { parseStatus(it) }
        val counterparty = call.request.queryParameters["counterparty"]
        val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceIn(1, 100) ?: 50
        val offset = call.request.queryParameters["offset"]?.toIntOrNull()?.coerceAtLeast(0) ?: 0

        val result =
            contractUseCase.list(
                ContractListQuery(status = status, counterparty = counterparty, limit = limit, offset = offset),
            )
        call.respond(ContractListResponse(contracts = result.contracts.map { it.toResponse() }, total = result.total))
    }
}

private fun Route.createContract(contractUseCase: ContractUseCase) {
    post {
        val request = call.receive<CreateContractRequest>()
        val command =
            CreateContractCommand(
                title = request.title,
                counterparty = request.counterparty,
                startDate = parseDate(request.startDate, "startDate"),
                endDate = request.endDate?.let { parseDate(it, "endDate") },
                autoRenewal = request.autoRenewal,
                status = request.status.toContractStatus(),
            )
        val contract = contractUseCase.create(command)
        call.respond(HttpStatusCode.Created, contract.toResponse())
    }
}

private fun Route.getContract(contractUseCase: ContractUseCase) {
    get("/{id}") {
        val id = parseContractId(call.parameters["id"]!!)
        val contract = contractUseCase.getById(id)
        call.respond(contract.toResponse())
    }
}

private fun Route.updateContract(contractUseCase: ContractUseCase) {
    patch("/{id}") {
        val id = parseContractId(call.parameters["id"]!!)
        val request = call.receive<UpdateContractRequest>()
        val command =
            UpdateContractCommand(
                title = request.title,
                counterparty = request.counterparty,
                startDate = request.startDate?.let { parseDate(it, "startDate") },
                endDate = request.endDate?.let { parseDate(it, "endDate") },
                autoRenewal = request.autoRenewal,
                status = request.status?.toContractStatus(),
            )
        val contract = contractUseCase.update(id, command)
        call.respond(contract.toResponse())
    }
}

private fun Route.deleteContract(contractUseCase: ContractUseCase) {
    delete("/{id}") {
        val id = parseContractId(call.parameters["id"]!!)
        contractUseCase.delete(id)
        call.respond(HttpStatusCode.NoContent)
    }
}

private fun parseDate(
    value: String,
    field: String,
): LocalDate =
    try {
        LocalDate.parse(value)
    } catch (_: DateTimeParseException) {
        throw InvalidRequestException(field, "must be a valid date (YYYY-MM-DD)")
    }

private fun parseContractId(value: String): ContractId =
    try {
        ContractId.fromString(value)
    } catch (_: IllegalArgumentException) {
        throw InvalidRequestException("id", "must be a valid UUID")
    }

private fun parseStatus(value: String): ContractStatus =
    try {
        ContractStatus.valueOf(value.uppercase())
    } catch (_: IllegalArgumentException) {
        throw InvalidRequestException("status", "must be one of: ACTIVE, EXPIRED, CANCELLED")
    }
