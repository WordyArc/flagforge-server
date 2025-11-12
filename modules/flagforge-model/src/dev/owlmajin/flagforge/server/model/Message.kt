package dev.owlmajin.flagforge.server.model

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.Instant
import kotlin.uuid.Uuid


data class MessageHeader(
    val id: String,
    val kind: MessageKind,
    val aggregateId: String,
    val aggregateType: AggregateType,
    val actorId: String?,
    val timestamp: Instant,
    val correlationId: String? = null,
)

data class Message<T : MessagePayload>(
    val header: MessageHeader,
    val payload: T,
)

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "payloadType",
)
sealed interface MessagePayload

sealed interface CommandPayload : MessagePayload {
    val expectedVersion: Long?
}

sealed interface EventPayload : MessagePayload {
    val version: Long
    val commandId: String
}

enum class AggregateType {
    PROJECT,
    ENVIRONMENT,
    SEGMENT,
    FLAG,
    SDK_KEY,
}

enum class MessageKind {
    COMMAND,
    EVENT,
}

typealias CommandMessage<T> = Message<T>
typealias EventMessage<T> = Message<T>

fun <T : CommandPayload> commandMessage(
    aggregateId: String,
    aggregateType: AggregateType,
    actorId: String,
    payload: T,
    timestamp: Instant = Instant.now(),
    correlationId: String? = null,
    messageId: String = Uuid.random().toString(),
): CommandMessage<T> = Message(
    header = MessageHeader(
        id = messageId,
        kind = MessageKind.COMMAND,
        aggregateId = aggregateId,
        aggregateType = aggregateType,
        actorId = actorId,
        timestamp = timestamp,
        correlationId = correlationId,
    ),
    payload = payload,
)

fun <T : EventPayload> eventMessage(
    aggregateId: String,
    aggregateType: AggregateType,
    actorId: String?,
    payload: T,
    timestamp: Instant = Instant.now(),
    correlationId: String? = null,
    messageId: String = Uuid.random().toString(),
): EventMessage<T> = Message(
    header = MessageHeader(
        id = messageId,
        kind = MessageKind.EVENT,
        aggregateId = aggregateId,
        aggregateType = aggregateType,
        actorId = actorId,
        timestamp = timestamp,
        correlationId = correlationId,
    ),
    payload = payload,
)
