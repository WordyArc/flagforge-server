package dev.owlmajin.flagforge.server.common.kafka

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
class PersistenceTopicConfiguration {

    @Bean
    fun persistenceProperties() = PersistenceProperties()

    @Bean
    fun topicInitialization(
        topicGroups: List<TopicGroup>,
        persistenceProperties: PersistenceProperties,
    ) {

    }

    class TopicGroup(
        val name: String,
        val topics: (PersistenceProperties) -> Set<TopicProperties>
    )

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
