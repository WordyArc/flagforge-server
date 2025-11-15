package dev.owlmajin.flagforge.server.processor.pipeline.flag

import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.processor.pipeline.debugLog
import org.apache.kafka.streams.kstream.KStream

fun KStream<String, Message<*>>.logIncomingCommands(): KStream<String, Message<*>> =
    debugLog("incoming-command") { key, value ->
        "key=$key, kind=${value.header.kind}, payloadType=${value.payload::class.simpleName}"
    }

fun KStream<String, Message<*>>.logIncomingEvents(): KStream<String, Message<*>> =
    debugLog("incoming-event") { key, value ->
        "key=$key, kind=${value.header.kind}, payloadType=${value.payload::class.simpleName}"
    }
