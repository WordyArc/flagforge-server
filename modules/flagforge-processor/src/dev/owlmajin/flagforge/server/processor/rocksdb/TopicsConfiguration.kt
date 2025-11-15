package dev.owlmajin.flagforge.server.processor.rocksdb

import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.model.Message
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

data class KafkaTopic<K, V>(
    val name: String,
    val keySerde: Serde<K>,
    val valueSerde: Serde<V>,
)

fun <K, V> topic(
    name: String,
    keySerde: Serde<K>,
    valueSerde: Serde<V>,
): KafkaTopic<K, V> = KafkaTopic(name, keySerde, valueSerde)

data class Topics(
    val commands: KafkaTopic<String, Message<*>>,
    val events: KafkaTopic<String, Message<*>>,
    val flagState: KafkaTopic<String, FlagState>,
    val projectState: KafkaTopic<String, Any>,
    val envState: KafkaTopic<String, Any>,
    val segmentState: KafkaTopic<String, Any>,
    val flagKeyIndex: KafkaTopic<String, String>,
)

@Configuration
class TopicsConfiguration(
    private val persistenceProperties: PersistenceProperties,
    private val messageSerde: Serde<Message<*>>,
    private val flagStateSerde: Serde<FlagState>,
    private val anySerde: Serde<Any>,
) {

    @Bean
    fun topics(): Topics {
        val stringSerde = Serdes.String()

        return Topics(
            commands = topic(
                name = persistenceProperties.commandMessages.effectiveName,
                keySerde = stringSerde,
                valueSerde = messageSerde,
            ),
            events = topic(
                name = persistenceProperties.eventMessages.effectiveName,
                keySerde = stringSerde,
                valueSerde = messageSerde,
            ),
            flagState = topic(
                name = persistenceProperties.flagState.effectiveName,
                keySerde = stringSerde,
                valueSerde = flagStateSerde,
            ),
            projectState = topic(
                name = persistenceProperties.projectState.effectiveName,
                keySerde = stringSerde,
                valueSerde = anySerde,
            ),
            envState = topic(
                name = persistenceProperties.envState.effectiveName,
                keySerde = stringSerde,
                valueSerde = anySerde,
            ),
            segmentState = topic(
                name = persistenceProperties.segmentState.effectiveName,
                keySerde = stringSerde,
                valueSerde = anySerde,
            ),
            flagKeyIndex = topic(
                name = "flag-key-index",
                keySerde = stringSerde,
                valueSerde = stringSerde,
            ),
        )
    }
}

