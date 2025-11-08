package dev.owlmajin.flagforge.server.common.kafka.serde

import org.apache.kafka.common.header.Headers
import org.springframework.kafka.support.mapping.DefaultJacksonJavaTypeMapper
import tools.jackson.databind.JavaType

class KafkaJacksonTypeMapper : DefaultJacksonJavaTypeMapper() {

    override fun fromJavaType(javaType: JavaType, headers: Headers) {
        removeHeaders(headers)
        super.fromJavaType(javaType, headers)

        val headerName = classIdFieldName
        addHeader(headers, headerName, javaType.rawClass)
    }
}
