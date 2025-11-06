package dev.owlmajin.flagforge.server.common.kafka.topic

import dev.owlmajin.flagforge.server.common.kafka.CONTENT_TYPE_HEADER_NAME
import dev.owlmajin.flagforge.server.common.kafka.CONTENT_TYPE_JSON
import dev.owlmajin.flagforge.server.common.kafka.DATA_SCHEMA_HEADER_NAME
import org.apache.kafka.common.header.Header
import org.apache.kafka.common.header.internals.RecordHeader
import java.net.URI

fun TopicProperties.headerList(): MutableList<Header> = mutableListOf(
    RecordHeader(DATA_SCHEMA_HEADER_NAME, header.toByteArray()),
    RecordHeader(CONTENT_TYPE_HEADER_NAME, CONTENT_TYPE_JSON.toByteArray())
)

fun TopicProperties.dataSchema() = URI(header)