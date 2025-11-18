package dev.owlmajin.flagforge.server.model.api.v1

data class CommandResponse(
    val apiVersion: String = "v1",
    val status: String = "ACCEPTED",
    val commandId: String,
    val resourceName: String,
)

data class ErrorResponse(
    val apiVersion: String = "v1",
    val status: String,
    val errorCode: String,
    val message: String,
)
