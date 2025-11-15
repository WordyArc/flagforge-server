package dev.owlmajin.flagforge.server.processor.config

import dev.owlmajin.flagforge.server.processor.topology.StreamsTopology
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Configuration

@Configuration
class TopologyConfiguration(private val topologies: List<StreamsTopology>) {
    private val log = KotlinLogging.logger { javaClass }

    @PostConstruct
    fun kafkaStreamsTopology() {
        topologies.forEach { it.configure() }
        log.info { "Kafka Streams topology built with ${topologies.size} pipelines" }
    }

}
