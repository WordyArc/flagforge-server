package dev.owlmajin.flagforge.server.processor.pipeline

import dev.owlmajin.flagforge.server.processor.rocksdb.Topology

interface StreamsPipeline {
    fun build(topology: Topology)
}