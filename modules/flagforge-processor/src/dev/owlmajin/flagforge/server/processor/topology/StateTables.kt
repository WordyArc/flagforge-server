package dev.owlmajin.flagforge.server.processor.topology

import dev.owlmajin.flagforge.server.model.FlagState
import dev.owlmajin.flagforge.server.processor.streams.Topics
import dev.owlmajin.flagforge.server.processor.streams.table
import org.apache.kafka.streams.StreamsBuilder
import org.apache.kafka.streams.kstream.KTable
import org.springframework.stereotype.Component
import kotlin.concurrent.Volatile

@Component
class StateTables(private val topics: Topics) {

    private val lock = Object()

    @Volatile
    private var flagStateTable: KTable<String, FlagState>? = null

    fun flagState(builder: StreamsBuilder): KTable<String, FlagState> {
        flagStateTable?.let { return it }
        synchronized(this) {
            flagStateTable?.let { return it }
            val table = builder.table(topics.flagState)
            flagStateTable = table
            return table
        }
    }

    @Volatile
    private var projectStateTable: KTable<String, Any>? = null

    fun projectState(builder: StreamsBuilder): KTable<String, Any> {
        projectStateTable?.let { return it }
        synchronized(this) {
            projectStateTable?.let { return it }
            val table = builder.table(topics.projectState)
            projectStateTable = table
            return table
        }
    }

    @Volatile
    private var envStateTable: KTable<String, Any>? = null

    fun envState(builder: StreamsBuilder): KTable<String, Any> {
        envStateTable?.let { return it }
        synchronized(this) {
            envStateTable?.let { return it }
            val table = builder.table(topics.envState)
            envStateTable = table
            return table
        }
    }

    @Volatile
    private var segmentStateTable: KTable<String, Any>? = null

    fun segmentState(builder: StreamsBuilder): KTable<String, Any> {
        segmentStateTable?.let { return it }
        synchronized(this) {
            segmentStateTable?.let { return it }
            val table = builder.table(topics.segmentState)
            segmentStateTable = table
            return table
        }
    }

}