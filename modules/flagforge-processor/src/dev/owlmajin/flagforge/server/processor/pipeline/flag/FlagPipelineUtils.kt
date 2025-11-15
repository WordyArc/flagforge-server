package dev.owlmajin.flagforge.server.processor.pipeline.flag

import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.processor.handler.CommandResult
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.kstream.KStream

private val klog = KotlinLogging.logger {}

fun logIncomingCommands(commands: KStream<String, Message<*>>): KStream<String, Message<*>> =
    commands.peek { key, value ->
        klog.debug {
            "Incoming message: key=$key, kind=${value.header.kind}, payloadType=${value.payload::class.simpleName}"
        }
    }

fun logIncomingEvents(events: KStream<String, Message<*>>): KStream<String, Message<*>> =
    events.peek { key, value ->
    klog.debug { "Incoming event: key=$key, kind=${value.header.kind}, payloadType=${value.payload::class.simpleName}" }
}

fun dropIgnoredCommands(resultStream: KStream<String, CommandResult>): KStream<String, CommandResult> =
    resultStream.filter { _, result -> result !is CommandResult.Ignored }