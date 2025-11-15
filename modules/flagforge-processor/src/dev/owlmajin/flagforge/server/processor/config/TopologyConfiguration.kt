package dev.owlmajin.flagforge.server.processor.config

import dev.owlmajin.flagforge.server.processor.pipeline.StreamsPipeline
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.StreamsBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TopologyConfiguration(private val pipelines: List<StreamsPipeline>) {
    private val klog = KotlinLogging.logger { javaClass }

    @Bean
    fun kafkaStreamsTopology(builder: StreamsBuilder) {
        pipelines.forEach { pipeline ->
            pipeline.build(builder)
        }
        klog.info { "Kafka Streams topology built with ${pipelines.size} pipelines" }
    }

}

