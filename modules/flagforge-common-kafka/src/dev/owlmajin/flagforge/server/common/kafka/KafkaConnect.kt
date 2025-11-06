package dev.owlmajin.flagforge.server.common.kafka

import org.apache.kafka.clients.admin.AdminClient
import org.apache.kafka.clients.admin.AdminClientConfig
import org.apache.kafka.clients.admin.KafkaAdminClient
import org.apache.kafka.clients.admin.NewTopic
import org.springframework.boot.kafka.autoconfigure.KafkaProperties
import org.springframework.kafka.core.KafkaAdmin
import kotlin.uuid.Uuid

private const val KAFKA_ADMIN = "kafka-admin"
private const val KAFKA_ADMIN_CLIENT = "kafka-admin-client"

class KafkaConnect(kafkaProperties: KafkaProperties): AutoCloseable {

    val kafkaAdmin: KafkaAdmin = KafkaAdmin(kafkaProperties.buildAdminProperties().also {
        it[AdminClientConfig.CLIENT_ID_CONFIG] = "${it[AdminClientConfig.CLIENT_ID_CONFIG]}-$KAFKA_ADMIN-${Uuid.random()}"
    })

    val adminClient: AdminClient = KafkaAdminClient.create(kafkaProperties.buildAdminProperties().also {
        it[AdminClientConfig.CLIENT_ID_CONFIG] = "${it[AdminClientConfig.CLIENT_ID_CONFIG]}-$KAFKA_ADMIN_CLIENT-${Uuid.random()}"
    })

    internal fun createOrUpdateTopic(topics: Set<NewTopic>) {
        if (topics.isEmpty()) return
        kafkaAdmin.createOrModifyTopics(*topics.toTypedArray())
    }

    internal fun listExistingTopicNames(): Set<String> {
        return adminClient.listTopics().names().get()
    }

    internal fun getNotExistingTopics(topicNames: Set<String>): Set<String> {
        val existing = listExistingTopicNames()
        return topicNames - existing
    }



    override fun close() {
        adminClient.close()
    }

}
