package dev.owlmajin.flagforge.server.processor.pipeline

import org.apache.kafka.streams.StreamsBuilder

interface StreamsPipeline {
    fun build(builder: StreamsBuilder)
}