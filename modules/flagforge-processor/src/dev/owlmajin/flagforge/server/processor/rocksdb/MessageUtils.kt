package dev.owlmajin.flagforge.server.processor.rocksdb

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.CommandPayload
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.EventPayload
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.MessageKind
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable

inline fun <reified P: CommandPayload> KStream<String, Message<*>>.commandsOf(): KStream<String, CommandMessage<P>> =
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

