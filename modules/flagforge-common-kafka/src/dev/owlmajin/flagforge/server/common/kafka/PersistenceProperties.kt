package dev.owlmajin.flagforge.server.common.kafka

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.kafka.autoconfigure.KafkaProperties

private const val DELETE_POLICY = "delete"
private const val COMPACT_POLICY = "compact"

@ConfigurationProperties("app.persistence")
class PersistenceProperties {
    val enabled: Boolean = true
    val autoCreateTopics: Boolean = true
    val kafka: KafkaProperties = KafkaProperties()

    var flagCommands = TopicProperties()
        set(value) {
            field = value.withDefault(field)
        }
    var flagEvents = TopicProperties()
        set(value) {
            field = value.withDefault(field)
        }
    var flagState = TopicProperties()
        set(value) {
            field = value.withDefault(field)
        }
    var segmentState = TopicProperties()
        set(value) {
            field = value.withDefault(field)
        }
    var envState = TopicProperties()
        set(value) {
            field = value.withDefault(field)
        }
    var projectState = TopicProperties()
        set(value) {
            field = value.withDefault(field)
        }
    var sdkKeys = TopicProperties()
        set(value) {
            field = value.withDefault(field)
        }

    init {
        flagCommands = flagCommands.withDefault(
            "flag-commands",
            "flagCommandsTopic",
            DELETE_POLICY,
            "runtime"
        )
        flagEvents = flagEvents.withDefault(
            "flag-events",
            "flagEventsTopic",
            DELETE_POLICY,
            "runtime"
        )
        flagState = flagState.withDefault(
            "flag-state",
            "flagStateTopic",
            COMPACT_POLICY,
            "runtime"
        )
        segmentState = segmentState.withDefault(
            "segment-state",
            "segmentStateTopic",
            COMPACT_POLICY,
            "runtime"
        )
        envState = envState.withDefault(
            "env-state",
            "envStateTopic",
            COMPACT_POLICY,
            "runtime"
        )
        projectState = flagState.withDefault(
            "project-state",
            "projectStateTopic",
            COMPACT_POLICY,
            "runtime"
        )
        sdkKeys = flagState.withDefault(
            "sdk-keys",
            "sdkKeysTopic",
            COMPACT_POLICY,
            "runtime"
        )
    }
}