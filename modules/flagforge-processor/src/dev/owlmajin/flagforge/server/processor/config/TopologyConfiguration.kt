package dev.owlmajin.flagforge.server.processor.config

import dev.owlmajin.flagforge.server.processor.topology.StreamsTopology
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.StreamsBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TopologyConfiguration(private val topologies: List<StreamsTopology>) {
    private val klog = KotlinLogging.logger { javaClass }

    @Bean
    fun kafkaStreamsTopology(builder: StreamsBuilder) {
        topologies.forEach { it.configure(builder) }
        klog.info { "Kafka Streams topology built with ${topologies.size} pipelines" }
    }

}

