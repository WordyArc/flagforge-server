package dev.owlmajin.flagforge.server.common.kafka.producer

import dev.owlmajin.flagforge.server.common.kafka.topic.TopicProperties
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.clients.producer.ProducerConfig
import org.slf4j.LoggerFactory
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaOperations
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

@Component
class KafkaProducerFactory(private val kafkaProperties: KafkaProperties): AutoCloseable {
    private val log = LoggerFactory.getLogger(javaClass)

    private val factories = ConcurrentHashMap<String, DefaultKafkaProducerFactory<*, *>>()


    fun <K : Any, V : Any> createProducer(topic: TopicProperties): KafkaTopicProducer<K, V> {
        val template = createTemplate<K, V>(topic)
        return KafkaTopicProducer(topic, template)
    }

    private fun <K : Any, V : Any> createTemplate(topic: TopicProperties): KafkaOperations<K, V> {
        val factory = factories.computeIfAbsent(topic.effectiveName) {
            val props = buildProducerProps(topic)
            log.info("Creating Kafka producer factory for topic=${topic.effectiveName} with client.id=${props[ProducerConfig.CLIENT_ID_CONFIG]}")
            DefaultKafkaProducerFactory<Any, Any>(props)
        }

        @Suppress("UNCHECKED_CAST")
        return KafkaTemplate(factory as ProducerFactory<K, V>)
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
