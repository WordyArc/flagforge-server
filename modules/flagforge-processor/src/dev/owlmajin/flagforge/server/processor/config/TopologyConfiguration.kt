package dev.owlmajin.flagforge.server.processor.config

import dev.owlmajin.flagforge.server.processor.pipeline.StreamsPipeline
import dev.owlmajin.flagforge.server.processor.rocksdb.Topics
import dev.owlmajin.flagforge.server.processor.rocksdb.builder
import dev.owlmajin.flagforge.server.processor.rocksdb.topology
import io.github.oshai.kotlinlogging.KotlinLogging
import org.apache.kafka.streams.StreamsBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TopologyConfiguration(
    private val topics: Topics,
    private val pipelines: List<StreamsPipeline>,
) {
    private val klog = KotlinLogging.logger { javaClass }

    @Bean
    fun kafkaStreamsTopology(builder: StreamsBuilder): Unit =
        builder.topology(topics) {
            pipelines.forEach { pipeline ->
                pipeline.build(this)
            }
        }



}

