package dev.owlmajin.flagforge.server.common.kafka.producer

import org.apache.kafka.clients.producer.ProducerConfig
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import kotlin.uuid.Uuid

private const val DEFAULT_CLIENT_ID_PREFIX = "flagforge-producer"

internal fun KafkaProperties.buildProducerPropertiesWithUniqueClientId(clientSuffix: String): MutableMap<String, Any> {
    val props = buildProducerProperties().toMutableMap()

    val prefix = (props[ProducerConfig.CLIENT_ID_CONFIG] as? String)
        ?.takeIf { it.isNotBlank() }
        ?: DEFAULT_CLIENT_ID_PREFIX

    props[ProducerConfig.CLIENT_ID_CONFIG] = "$prefix-$clientSuffix-${Uuid.random()}"

    return props
}
