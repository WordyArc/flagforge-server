package dev.owlmajin.flagforge.server.common.kafka.producer

import dev.owlmajin.flagforge.server.common.kafka.CONTENT_TYPE_HEADER_NAME
import dev.owlmajin.flagforge.server.common.kafka.CONTENT_TYPE_JSON
import dev.owlmajin.flagforge.server.common.kafka.DATA_SCHEMA_HEADER_NAME
import dev.owlmajin.flagforge.server.common.kafka.topic.TopicProperties
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import java.net.URI

fun TopicProperties.defaultHeaders() = mutableListOf<Header>().apply {
        add(RecordHeader(DATA_SCHEMA_HEADER_NAME, header.toByteArray()))
        add(RecordHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_JSON.toByteArray()))
    }

fun TopicProperties.dataSchemaUri(): URI = URI(header)
