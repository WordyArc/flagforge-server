package dev.owlmajin.flagforge.server.processor.config

import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.FlagAggregate
import dev.owlmajin.flagforge.server.model.FlagCommand
import dev.owlmajin.flagforge.server.model.FlagEvent
import dev.owlmajin.flagforge.server.processor.FlagProcessingResult
import dev.owlmajin.flagforge.server.processor.Processor
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
import org.springframework.kafka.support.serializer.JsonSerde as SpringKafkaJsonSerde

@Configuration
class TopologyConfiguration(
    private val persistenceProperties: PersistenceProperties,
    private val processor: Processor,
    private val commandSerde: Serde<Any>,
    private val flagAggregateSerde: SpringKafkaJsonSerde<FlagAggregate>,
    private val flagEventSerde: SpringKafkaJsonSerde<Any>,
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
        val flagState: KTable<String, FlagAggregate> =
            builder.table(
                flagStateTopic,
                Consumed.with(stringSerde, flagAggregateSerde),
            )

        val anySerde = SpringKafkaJsonSerde(Any::class.java).apply {
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

        val commands: KStream<String, Any> =
            builder.stream(
                commandsTopic,
                Consumed.with(stringSerde, commandSerde),
            ).peek { key, value ->
                log.info("RAW command from stream: key={}, type={}, class={}", key, value?.let { it::class.simpleName }, value?.javaClass)
                if (value is FlagCommand) {
                    log.info("Successfully deserialized FlagCommand: flagId={}, commandId={}", value.flagId, value.id)
                } else {
                    log.warn("Command is not FlagCommand but {}", value?.javaClass?.name)
                }
            }

        val results: KStream<String, FlagProcessingResult> =
            commands
                .filter { key, value -> 
                    val isFlagCommand = value is FlagCommand
                    if (!isFlagCommand) {
                        log.warn("Filtered out non-FlagCommand: key={}, class={}", key, value?.javaClass?.name)
                    }
                    isFlagCommand
                }
                .mapValues { value -> value as FlagCommand }
                .peek { key, command ->
                    log.info("Processing FlagCommand: key={}, commandType={}, flagId={}", key, command::class.simpleName, command.flagId)
                }
                .leftJoin(
                    flagState,
                    { command, currentState -> 
                        log.info("Processing command with state: command={}, currentState={}", command::class.simpleName, currentState?.id)
                        processor.process(command, currentState)
                    },
                )
                .peek { key, result ->
                    when (result) {
                        is FlagProcessingResult.Applied -> log.info("Command applied: key={}, event={}", key, result.event::class.simpleName)
                        is FlagProcessingResult.Rejected -> log.warn("Command rejected: key={}, reason={}", key, result.event)
                    }
                }

        // --- события ---

        results
            .mapValues { result ->
                when (result) {
                    is FlagProcessingResult.Applied -> result.event
                    is FlagProcessingResult.Rejected -> result.event
                }
            }
            .peek { key, event ->
                log.debug("Produce flag-event. key={}, type={}", key, event::class.simpleName)
            }
            .to(
                eventsTopic,
                Produced.with(stringSerde, flagEventSerde as SpringKafkaJsonSerde<FlagEvent>),
            )

        // --- состояние агрегатов ---

        results
            .mapValues { result ->
                when (result) {
                    is FlagProcessingResult.Applied -> result.newState
                    is FlagProcessingResult.Rejected -> null
                }
            }
            .peek { key, aggregate ->
                log.debug("Produce flag-state. key={}, isTombstone={}", key, aggregate == null)
            }
            .to(
                flagStateTopic,
                Produced.with(stringSerde, flagAggregateSerde),
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

        return results
    }
}
