package dev.owlmajin.flagforge.server.processor.streams

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.MessageKind
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.GlobalKTable
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Produced

val log = KotlinLogging.logger {}

fun <K, V> StreamsBuilder.stream(topic: TopicDescriptor<K, V>): KStream<K, V> =
    stream(topic.name, Consumed.with(topic.keySerde, topic.valueSerde))

fun <K, V> StreamsBuilder.table(topic: TopicDescriptor<K, V>): KTable<K, V> =
    table(topic.name, Consumed.with(topic.keySerde, topic.valueSerde))

fun <K, V> StreamsBuilder.globalTable(topic: TopicDescriptor<K, V>): GlobalKTable<K, V> =
    globalTable(topic.name, Consumed.with(topic.keySerde, topic.valueSerde))

infix fun <K, V> KStream<K, V>.publishTo(topic: TopicDescriptor<K, V>) {
    to(topic.name, Produced.with(topic.keySerde, topic.valueSerde))
}

infix fun <K, V> KStream<K, V?>.nullablePublishTo(topic: TopicDescriptor<K, V>) {
    @Suppress("UNCHECKED_CAST")
    val serde = topic.valueSerde as Serde<V?>
    to(topic.name, Produced.with(topic.keySerde, serde))
}


inline fun <reified P : CommandPayload> KStream<String, Message<*>>.commandsOf(): KStream<String, CommandMessage<P>> =
    filter { _, message ->
        message != null &&
                message.header.kind == MessageKind.COMMAND &&
                message.payload is P
    }.mapValues { message -> message as CommandMessage<P> }

inline fun <reified P : EventPayload> KStream<String, Message<*>>.eventsOf(): KStream<String, EventMessage<P>> =
    filter { _, message ->
        message != null &&
                message.header.kind == MessageKind.EVENT &&
                message.payload is P
    }.mapValues { message -> message as EventMessage<P> }


fun <K, C, S, R> KStream<K, C>.withState(
    table: KTable<K, S>,
    joiner: (C, S?) -> R,
): KStream<K, R> = leftJoin(table, joiner)


fun KStream<String, CommandResult>.toDomainEvents(): KStream<String, Message<*>> =
    flatMapValues { result ->
        when (result) {
            is CommandResult.Applied -> listOf(result.event as Message<*>)
            is CommandResult.Rejected -> listOf(result.event as Message<*>)
            CommandResult.Ignored -> emptyList()
        }
    }

inline fun <K, V> KStream<K, V>.debugLog(
    label: String,
    crossinline message: (key: K, value: V) -> String,
): KStream<K, V> =
    peek { key, value -> log.debug { "[$label] ${message(key, value)}" } }