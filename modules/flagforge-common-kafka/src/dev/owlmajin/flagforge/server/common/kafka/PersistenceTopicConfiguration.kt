package dev.owlmajin.flagforge.server.common.kafka

import org.apache.kafka.clients.admin.NewTopic
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
        if (!persistenceProperties.enabled || !persistenceProperties.autoCreateTopics) {
            log.info("auto create topics is disabled (enabled=${persistenceProperties.enabled}, autoCreateTopics=${persistenceProperties.autoCreateTopics})")
            return@ApplicationRunner
        }

        val autoCreate: Boolean = persistenceProperties.autoCreateTopics

        topicGroups.forEach { group ->
            val topics = group.topics(persistenceProperties)

            TopicGroupStarter.ofTopics(
                group.name,
                kafkaConnect,
                true,
                autoCreate,
                topics,

            )
        }


        val allTopicsFromGroup: Set<TopicProperties> = topicGroups.flatMap { it.topics(persistenceProperties) }.toSet()
        val existing = kafkaConnect.listExistingTopicNames()
        val newTopics: Set<NewTopic> = allTopicsFromGroup
            .filter { it.name !in existing }
            .map { it.toNewTopic() }
            .toSet()

        log.info("Creating kafka topics: ${newTopics.map { it.name() }}")
        kafkaConnect.createOrUpdateTopic(newTopics)
    }

    @Profile("processor")
    @Configuration
    class ProcessorConfiguration {
        @Bean
        fun topicGroup() = TopicGroup("persistence") {
            buildSet {
                add(it.flagCommands)
                add(it.flagEvents)
                add(it.flagState)
                add(it.segmentState)
                add(it.envState)
                add(it.projectState)
                add(it.sdkKeys)
            }
        }
    }

}
