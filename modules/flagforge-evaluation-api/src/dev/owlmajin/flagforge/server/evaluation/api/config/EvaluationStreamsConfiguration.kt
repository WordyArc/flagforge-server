package dev.owlmajin.flagforge.server.evaluation.api.config

import dev.owlmajin.flagforge.server.common.kafka.topic.PersistenceProperties
import dev.owlmajin.flagforge.server.evaluation.api.FLAG_INDEX_STORE_NAME
import dev.owlmajin.flagforge.server.evaluation.api.FLAG_INDEX_TOPIC_NAME
import dev.owlmajin.flagforge.server.evaluation.api.FLAG_STATE_STORE_NAME
import dev.owlmajin.flagforge.server.model.FlagState
import org.apache.kafka.common.serialization.Serde
import org.apache.kafka.common.serialization.Serdes
import org.apache.kafka.common.utils.Bytes
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KTable
import org.apache.kafka.streams.kstream.Materialized
import org.apache.kafka.streams.state.KeyValueStore
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class EvaluationStreamsConfiguration(
    private val persistenceProperties: PersistenceProperties,
    private val flagStateSerde: Serde<FlagState>,
) {

    @Bean
    fun flagStateTable(builder: StreamsBuilder): KTable<String, FlagState> {
        val stringSerde = Serdes.String()
        val topicName = persistenceProperties.flagState.effectiveName

        return builder.table(
            topicName,
            Materialized.`as`<String, FlagState, KeyValueStore<Bytes, ByteArray>>(FLAG_STATE_STORE_NAME)
                .withKeySerde(stringSerde)
                .withValueSerde(flagStateSerde),
        )
    }

    @Bean
    fun flagIndexTable(builder: StreamsBuilder): KTable<String, String> {
        val stringSerde = Serdes.String()

        return builder.table(
            FLAG_INDEX_TOPIC_NAME,
            Materialized.`as`<String, String, KeyValueStore<Bytes, ByteArray>>(FLAG_INDEX_STORE_NAME)
                .withKeySerde(stringSerde)
                .withValueSerde(stringSerde),
        )
    }

}
