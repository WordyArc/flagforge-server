package dev.owlmajin.flagforge.server.common.kafka.producer

import dev.owlmajin.flagforge.server.common.kafka.serde.OmniKafkaSerializer
import dev.owlmajin.flagforge.server.common.kafka.topic.TopicProperties
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.LoggerFactory
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class OmniProducerFactory(private val kafkaProperties: KafkaProperties) : AutoCloseable {
    private val log = LoggerFactory.getLogger(javaClass)

    private val factories = ConcurrentHashMap<String, ProducerFactory<Any, Any>>()

    fun createTopicProducer(topic: TopicProperties): TopicProducer<Any, Any> {
        val normalizedTopic = topic.applyDefaults()
        return TopicProducer(normalizedTopic, createTemplate(normalizedTopic))
    }

    fun create(topic: TopicProperties): KafkaOperations<Any, Any> {
        val normalizedTopic = topic.applyDefaults()
        return createTemplate(normalizedTopic)
    }

    private fun createTemplate(topic: TopicProperties): KafkaOperations<Any, Any> {
        val factory = factories.computeIfAbsent(topic.effectiveName) {
            val props = buildProducerProps(topic)
            log.info("Creating Kafka producer factory for topic=${topic.effectiveName} with client.id=${props[ProducerConfig.CLIENT_ID_CONFIG]}")

            @Suppress("UNCHECKED_CAST")
            DefaultKafkaProducerFactory(
                props,
                StringSerializer() as Serializer<Any>,
                OmniKafkaSerializer() as Serializer<Any>
            )
        }

        return KafkaTemplate(factory)
    }

    private fun buildProducerProps(topic: TopicProperties): Map<String, Any> {
        val base = kafkaProperties.buildProducerPropertiesWithUniqueClientId(topic.effectiveName)
        val overrides = topic.producer?.buildProperties().orEmpty()

        val props = base.toMutableMap().apply {
            putAll(overrides)
        }

        return props
    }

    override fun close() {
        factories.values.forEach { factory ->
            try {
                factory.destroy()
            } catch (e: Exception) {
                log.warn("Error while closing Kafka producer factory", e)
            }
        }
        factories.clear()
    }
}
