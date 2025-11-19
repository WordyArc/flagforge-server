package dev.owlmajin.flagforge.server.processor.topology

import dev.owlmajin.flagforge.server.model.environment.EnvironmentState
import dev.owlmajin.flagforge.server.model.flag.FlagState
import dev.owlmajin.flagforge.server.model.project.ProjectState
import dev.owlmajin.flagforge.server.processor.streams.Topics
import dev.owlmajin.flagforge.server.processor.streams.globalTable
import dev.owlmajin.flagforge.server.processor.streams.table
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.GlobalKTable
import org.apache.kafka.streams.kstream.KTable
import org.springframework.stereotype.Component

@Component
class StateTables(
    private val topics: Topics,
    private val builder: StreamsBuilder,
    ) {

    val flagState: KTable<String, FlagState> by lazy { builder.table(topics.flagState) }

    val projectState: KTable<String, ProjectState> by lazy { builder.table(topics.projectState) }

    val projectKeyIndex: GlobalKTable<String, String> by lazy { builder.globalTable(topics.projectKeyIndex) }

    val envState: KTable<String, EnvironmentState> by lazy { builder.table(topics.envState) }

    val segmentState: KTable<String, *> by lazy { builder.table(topics.segmentState) }

}