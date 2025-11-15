package dev.owlmajin.flagforge.server.processor.topology.flag

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.FlagCommandPayload
import dev.owlmajin.flagforge.server.model.FlagEventPayload
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.processor.handling.CommandResult
import dev.owlmajin.flagforge.server.processor.handling.EventResult
import org.apache.kafka.streams.kstream.KStream

typealias FlagRawMessageStream = KStream<String, Message<*>>
typealias FlagCommandStream = KStream<String, CommandMessage<FlagCommandPayload>>
typealias FlagEventStream = KStream<String, EventMessage<FlagEventPayload>>

typealias FlagCommandResultStream = KStream<String, CommandResult>
typealias FlagEventResultStream = KStream<String, EventResult>