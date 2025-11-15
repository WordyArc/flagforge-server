package dev.owlmajin.flagforge.server.processor.topology

import org.apache.kafka.streams.StreamsBuilder

interface StreamsTopology {
    fun configure(builder: StreamsBuilder)
}