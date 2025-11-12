package dev.owlmajin.flagforge.server.processor.config

import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.CommandMessage
import dev.owlmajin.flagforge.server.model.EventMessage
import dev.owlmajin.flagforge.server.model.FlagEventPayload
import dev.owlmajin.flagforge.server.model.FlagCommandPayload
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.model.Message
import dev.owlmajin.flagforge.server.model.MessageKind
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.handler.MessageHandlingResult
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.streams.KeyValue
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.Consumed
import org.apache.kafka.streams.kstream.KStream
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Produced
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.support.serializer.JacksonJsonSerde

@Configuration
class TopologyConfiguration(
    private val persistenceProperties: PersistenceProperties,
    private val messageProcessor: MessageProcessor,
    private val messageSerde: Serde<Message<*>>,
    private val flagStateSerde: JacksonJsonSerde<FlagState>,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun flagCommandKStream(builder: StreamsBuilder): KStream<String, *> {
        val commandsTopic = persistenceProperties.commandMessages.effectiveName
    val eventsTopic = persistenceProperties.eventMessages.effectiveName
    val flagStateTopic = persistenceProperties.flagState.effectiveName
        val projectStateTopic = persistenceProperties.projectState.effectiveName
        val envStateTopic = persistenceProperties.envState.effectiveName
        val segmentStateTopic = persistenceProperties.segmentState.effectiveName

        log.info(
            "Building processor topology. commandsTopic={}, eventsTopic={}, flagStateTopic={}, projectStateTopic={}, envStateTopic={}, segmentStateTopic={}",
            commandsTopic, eventsTopic, flagStateTopic, projectStateTopic, envStateTopic, segmentStateTopic,
        )

        val stringSerde = Serdes.String()


        // --- state ---
        val flagState: KTable<String, FlagState> =
            builder.table(
                flagStateTopic,
                Consumed.with(stringSerde, flagStateSerde),
            )

        val anySerde = JacksonJsonSerde(Any::class.java).apply {
            deserializer().addTrustedPackages("dev.owlmajin.flagforge.server.model", "*")
        }

        builder.table<String, Any>(
            projectStateTopic,
            Consumed.with(stringSerde, anySerde),
        )

        builder.table<String, Any>(
            envStateTopic,
            Consumed.with(stringSerde, anySerde),
        )

        builder.table<String, Any>(
            segmentStateTopic,
            Consumed.with(stringSerde, anySerde),
        )

        // --- команды ---

        val commands: KStream<String, Message<*>> =
            builder.stream(
                commandsTopic,
                Consumed.with(stringSerde, messageSerde),
            ).peek { key, value ->
                log.debug(
                    "Incoming message: key={}, kind={}, payloadType={}",
                    key,
                    value?.header?.kind,
                    value?.payload?.let { it::class.simpleName },
                )
            }

        val flagCommands: KStream<String, CommandMessage<FlagCommandPayload>> =
            commands
                .filter { _, message ->
                    message != null &&
                        message.header.kind == MessageKind.COMMAND &&
                        message.payload is FlagCommandPayload
                }
                .mapValues { message ->
                    @Suppress("UNCHECKED_CAST")
                    message as CommandMessage<FlagCommandPayload>
                }
                .peek { key, command ->
                    log.info(
                        "Processing flag command: key={}, flagId={}, payloadType={}, commandId={}",
                        key,
                        command.payload.flagId,
                        command.payload::class.simpleName,
                        command.header.id,
                    )
                }

        val commandResults: KStream<String, MessageHandlingResult.Command> =
            flagCommands
                .leftJoin(
                    flagState,
                    { command, currentState ->
                        log.info(
                            "Flag command with state: flagId={}, payloadType={}, currentVersion={}",
                            command.payload.flagId,
                            command.payload::class.simpleName,
                            currentState?.version,
                        )
                        messageProcessor.processCommand(command, currentState)
                    },
                )
                .peek { key, result ->
                    when (result) {
                        is MessageHandlingResult.CommandApplied<*> ->
                            log.info("Command applied: key={}, eventPayload={}", key, result.event.payload::class.simpleName)

                        is MessageHandlingResult.CommandRejected<*> ->
                            log.warn("Command rejected: key={}, payload={}", key, result.event.payload)

                        MessageHandlingResult.CommandIgnored ->
                            log.debug("Command ignored: key={}", key)
                    }
                }
                .filter { _, result -> result !is MessageHandlingResult.CommandIgnored }

        // --- события ---

        @Suppress("UNCHECKED_CAST")
        val eventMessageSerde = messageSerde as Serde<EventMessage<*>>

        commandResults
            .flatMapValues { result ->
                when (result) {
                    is MessageHandlingResult.CommandApplied<*> -> listOf(result.event as EventMessage<*>)
                    is MessageHandlingResult.CommandRejected<*> -> listOf(result.event as EventMessage<*>)
                    MessageHandlingResult.CommandIgnored -> emptyList()
                }
            }
            .peek { key, event ->
                log.debug("Produce flag-event. key={}, payloadType={}", key, event.payload::class.simpleName)
            }
            .to(
                eventsTopic,
                Produced.with(stringSerde, eventMessageSerde),
            )

        // --- применение событий и обновление состояния ---

        val events: KStream<String, Message<*>> =
            builder.stream(
                eventsTopic,
                Consumed.with(stringSerde, messageSerde),
            )
                .peek { key, value ->
                    log.debug(
                        "Incoming event: key={}, kind={}, payloadType={}",
                        key,
                        value?.header?.kind,
                        value?.payload?.let { it::class.simpleName },
                    )
                }

        val flagEvents: KStream<String, EventMessage<FlagEventPayload>> =
            events
                .filter { _, message ->
                    message != null &&
                        message.header.kind == MessageKind.EVENT &&
                        message.payload is FlagEventPayload
                }
                .mapValues { message ->
                    @Suppress("UNCHECKED_CAST")
                    message as EventMessage<FlagEventPayload>
                }

        val eventResults: KStream<String, MessageHandlingResult.Event> =
            flagEvents
                .leftJoin(
                    flagState,
                    { event, currentState ->
                        log.info(
                            "Apply flag event: flagId={}, payloadType={}, currentVersion={}",
                            event.payload.flagId,
                            event.payload::class.simpleName,
                            currentState?.version,
                        )
                        messageProcessor.processEvent(event, currentState)
                    },
                )
                .peek { key, result ->
                    when (result) {
                        is MessageHandlingResult.EventApplied<*> ->
                            log.info("Event applied: key={}, isTombstone={}", key, result.newState == null)

                        MessageHandlingResult.EventIgnored ->
                            log.debug("Event ignored: key={}", key)
                    }
                }
                .filter { _, result -> result !is MessageHandlingResult.EventIgnored }

        eventResults
            .mapValues { result ->
                val applied = result as MessageHandlingResult.EventApplied<*>
                @Suppress("UNCHECKED_CAST")
                applied.newState as FlagState?
            }
            .peek { key, aggregate ->
                log.debug("Produce flag-state. key={}, isTombstone={}", key, aggregate == null)
            }
            .to(
                flagStateTopic,
                Produced.with(stringSerde, flagStateSerde),
            )

        // --- индекс для Eval: (projectId, envKey, flagKey) -> flagId ---
        // Формат ключа: "<projectId>|<envKey>|<flagKey>" ; value = flagId
        val indexTopic = "flag-key-index"

        flagState
            .toStream()
            .filter { _, aggregate -> aggregate != null }
            .map { _, aggregate ->
                val a = requireNotNull(aggregate)
                KeyValue(
                    "${a.projectId}|${a.environmentKey}|${a.key}",
                    a.id,
                )
            }
            .peek { key, value ->
                log.debug("Produce flag-key-index. key={}, flagId={}", key, value)
            }
            .to(indexTopic, Produced.with(stringSerde, stringSerde))

        return commandResults
    }
}
