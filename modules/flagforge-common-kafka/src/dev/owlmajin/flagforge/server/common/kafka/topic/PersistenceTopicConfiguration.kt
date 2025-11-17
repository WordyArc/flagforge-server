package dev.owlmajin.flagforge.server.common.kafka.topic

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class PersistenceTopicConfiguration {

    private val log = LoggerFactory.getLogger(javaClass)

    class TopicGroup(
        val name: String,
        val topics: (PersistenceProperties) -> Set<TopicProperties>
    )

    @Bean
    fun topicInitializer(
        topicGroups: List<TopicGroup>,
        persistenceProperties: PersistenceProperties,
        kafkaConnect: KafkaConnect,
    ) = ApplicationRunner {
        if (!persistenceProperties.enabled) {
            log.info("persistence is disabled, skipping kafka topics initialization")
            return@ApplicationRunner
        }

        val isAutoCreateEnabled = persistenceProperties.autoCreateTopics

        topicGroups.forEach { group ->
            val topics = group.topics(persistenceProperties)

            TopicGroupStarter.ofTopics(
                groupName = group.name,
                kafkaConnect = kafkaConnect,
                topics = topics,
                isAutoCreateEnabled = isAutoCreateEnabled,
                shouldValidate = true,
            )
        }
    }

    @Profile("control-api")
    @Configuration
    class ControlAPiTopicsConfiguration {
        @Bean
        fun topicGroup() = TopicGroup("persistence") {
            buildSet {
                add(it.commandMessages)
            }
        }
    }

    @Profile("evaluation-api")
    @Configuration
    class EvaluationApiTopicsConfiguration {

        @Bean
        fun evaluationTopicGroup() = TopicGroup("persistence") {
            buildSet {
                add(it.flagState)
                add(it.segmentState)
                add(it.envState)
                add(it.projectState)
                add(it.sdkKeys)
                add(it.flagKeyIndex)
            }
        }
    }

    @Profile("processor")
    @Configuration
    class ProcessorTopicsConfiguration {
        @Bean
        fun topicGroup() = TopicGroup("persistence") {
            buildSet {
                add(it.commandMessages)
                add(it.eventMessages)
                add(it.flagState)
                add(it.segmentState)
                add(it.envState)
                add(it.projectState)
                add(it.sdkKeys)
                add(it.flagKeyIndex)
            }
        }
    }

}
