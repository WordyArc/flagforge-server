package dev.owlmajin.flagforge.server.processor.pipeline

import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.FlagCommandPayload
import dev.owlmajin.flagforge.server.model.FlagEventPayload
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handler.CommandResult
import dev.owlmajin.flagforge.server.processor.handler.EventResult
import dev.owlmajin.flagforge.server.processor.rocksdb.Topics
import dev.owlmajin.flagforge.server.processor.rocksdb.Topology
import dev.owlmajin.flagforge.server.processor.rocksdb.commandsOf
import dev.owlmajin.flagforge.server.processor.rocksdb.eventsOf
import dev.owlmajin.flagforge.server.processor.rocksdb.into
import dev.owlmajin.flagforge.server.processor.rocksdb.intoNullable
import dev.owlmajin.flagforge.server.processor.rocksdb.withState
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.springframework.stereotype.Component


@Component
class FlagPipeline(
    private val topics: Topics,
    private val messageProcessor: MessageProcessor,
) : StreamsPipeline {

    private val klog = KotlinLogging.logger { javaClass }

    override fun build(topology: Topology) = with(topology) {
        val flagState = initStateTables()

        val eventResults: KStream<String, EventResult> =
            chain("flag-pipeline") {
                from(topics.commands)
                    .step("log-incoming-commands") { logIncomingCommands(it) }
                    .step("extract-flag-commands") { extractFlagCommands(it) }
                    .step("handle-flag-commands") { handleFlagCommands(it, flagState) }
                    .step("drop-ignored-commands") { dropIgnoredCommands(it) }
                    .sideEffect("publish-flag-events") { publishFlagEvents(it) }
                    .step("read-flag-events") { readFlagEvents() }
                    .step("handle-flag-events") { handleFlagEvents(it, flagState) }
                    .build()
            }

        writeFlagState(eventResults)
        buildFlagIndex(eventResults)
    }

    private fun logIncomingCommands(
        commands: KStream<String, Message<*>>,
    ): KStream<String, Message<*>> =
        commands.peek { key, value ->
            klog.debug {
                "Incoming message: key=$key, kind=${value.header.kind}, payloadType=${value.payload::class.simpleName}"
            }
        }

    private fun extractFlagCommands(
        commands: KStream<String, Message<*>>,
    ): KStream<String, CommandMessage<FlagCommandPayload>> =
        commands
            .commandsOf<FlagCommandPayload>()
            .peek { key, command ->
                klog.info {
                    "Processing flag command: key=$key, flagId=${command.payload.flagId}, " +
                            "payloadType=${command.payload::class.simpleName}, commandId=${command.header.id}"
                }
            }



    private fun handleFlagCommands(
        flagCommands: KStream<String, CommandMessage<FlagCommandPayload>>,
        flagState: KTable<String, FlagState>,
    ): KStream<String, CommandResult> =
        flagCommands
            .withState(flagState) { command, currentState ->
                klog.info {
                    "Flag command with state: flagId=${command.payload.flagId}, " +
                            "payloadType=${command.payload::class.simpleName}, currentVersion=${currentState?.version}"
                }
                messageProcessor.processCommand(command, currentState)
            }
            .peek { key, result ->
                klog.debug {
                    when (result) {
                        is CommandResult.Applied ->
                            "Command applied: key=$key, eventPayload=${result.event?.payload?.let { it::class.simpleName }}"
                        is CommandResult.Rejected ->
                            "Command rejected: key=$key, payload=${result.event.payload}"
                        CommandResult.Ignored ->
                            "Command ignored: key=$key"
                    }
                }
            }


    private fun Topology.initStateTables(): KTable<String, FlagState> {
        val flagState = table(topics.flagState)

        table(topics.projectState)
        table(topics.envState)
        table(topics.segmentState)

        return flagState
    }


    private fun dropIgnoredCommands(
        resultStream: KStream<String, CommandResult>,
    ): KStream<String, CommandResult> =
        resultStream.filter { _, result -> result !is CommandResult.Ignored }



    private fun Topology.publishFlagEvents(
        commandResults: KStream<String, CommandResult>,
    ) {
        val eventMessageSerde = topics.events.valueSerde

        commandResults
            .flatMapValues { result ->
                when (result) {
                    is CommandResult.Applied -> listOf(result.event)
                    is CommandResult.Rejected -> listOf(result.event)
                    CommandResult.Ignored -> emptyList()
                }
            }
            .peek { key, event ->
                klog.debug { "Produce flag-event. key=$key, payloadType=${event.payload::class.simpleName}" }
            } into topics.events.copy(valueSerde = eventMessageSerde)
    }

    private fun Topology.readFlagEvents(): KStream<String, Message<*>> =
        stream(topics.events)
            .peek { key, value ->
                klog.debug {
                    "Incoming event: key=$key, kind=${value.header.kind}, payloadType=${value.payload::class.simpleName}"
                }
            }


    private fun handleFlagEvents(
        events: KStream<String, Message<*>>,
        flagState: KTable<String, FlagState>,
    ): KStream<String, EventResult> {
        val flagEvents: KStream<String, EventMessage<FlagEventPayload>> =
            events.eventsOf<FlagEventPayload>()

        return flagEvents
            .withState(flagState) { event, currentState ->
                klog.info {
                    "Apply flag event: flagId=${event.payload.flagId}, " +
                            "payloadType=${event.payload::class.simpleName}, currentVersion=${currentState?.version}"
                }
                messageProcessor.processEvent(event, currentState)
            }
            .peek { key, result ->
                klog.debug {
                    when (result) {
                        is EventResult.Applied<*> ->
                            "Event applied: key=$key, isTombstone=${result.newState == null}"
                        EventResult.Ignored ->
                            "Event ignored: key=$key"
                    }
                }
            }
            .filter { _, result -> result !is EventResult.Ignored }
    }


    private fun writeFlagState(eventResults: KStream<String, EventResult>) {
        eventResults
            .mapValues {
                when (it) {
                    is EventResult.Applied<*> -> it.newState as? FlagState
                    EventResult.Ignored -> null
                }
            }.peek { key, aggregate ->
                klog.debug { "Produce flag-state. key=$key, isTombstone=${aggregate == null}" }
            } intoNullable topics.flagState
    }


    private fun buildFlagIndex(eventResults: KStream<String, EventResult>) {
        eventResults
            .flatMap { _, result ->
                when (result) {
                    is EventResult.Applied<*> -> {
                        val before = result.previousState as? FlagState
                        val after = result.newState as? FlagState

                        when {
                            after != null -> {
                                listOf(
                                    KeyValue(
                                        "${after.projectId}|${after.environmentKey}|${after.key}",
                                        after.id,
                                    ),
                                )
                            }

                            before != null -> {
                                listOf(
                                    KeyValue(
                                        "${before.projectId}|${before.environmentKey}|${before.key}",
                                        null,
                                    ),
                                )
                            }

                            else -> emptyList()
                        }
                    }
                    EventResult.Ignored -> emptyList()
                }
            }
            .peek { key, value ->
                klog.debug {
                    if (value == null) {
                        "Tombstone flag-key-index. key=$key"
                    } else {
                        "Produce flag-key-index. key=$key, flagId=$value"
                    }
                }
            } intoNullable topics.flagKeyIndex
    }

}