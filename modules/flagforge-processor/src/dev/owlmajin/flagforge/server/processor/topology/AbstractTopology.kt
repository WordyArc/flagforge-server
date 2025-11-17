package dev.owlmajin.flagforge.server.processor.topology

import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.streams.Topics
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KTable
import org.springframework.beans.factory.annotation.Autowired

abstract class AbstractTopology : StreamsTopology {

    @Autowired
    protected lateinit var topics: Topics

    @Autowired
    protected lateinit var messageProcessor: MessageProcessor

    @Autowired
    private lateinit var stateTables: StateTables

    @Autowired
    protected lateinit var builder: StreamsBuilder

    protected val flagState: KTable<String, FlagState>
        get() = stateTables.flagState

    protected val projectState: KTable<String, *>
        get() = stateTables.projectState

    protected val envState: KTable<String, *>
        get() = stateTables.envState

    protected val segmentState: KTable<String, *>
        get() = stateTables.segmentState
}
