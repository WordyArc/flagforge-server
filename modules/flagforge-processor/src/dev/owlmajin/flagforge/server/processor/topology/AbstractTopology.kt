package dev.owlmajin.flagforge.server.processor.topology

import dev.owlmajin.flagforge.server.model.environment.EnvironmentState
import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.model.project.ProjectState
import dev.owlmajin.flagforge.server.processor.MessageProcessor
import dev.owlmajin.flagforge.server.processor.streams.Topics
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.GlobalKTable
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

    protected val projectState: KTable<String, ProjectState>
        get() = stateTables.projectState

    protected val projectKeyIndex: GlobalKTable<String, String>
        get() = stateTables.projectKeyIndex

    protected val envState: KTable<String, EnvironmentState>
        get() = stateTables.envState

    protected val segmentState: KTable<String, *>
        get() = stateTables.segmentState
}
