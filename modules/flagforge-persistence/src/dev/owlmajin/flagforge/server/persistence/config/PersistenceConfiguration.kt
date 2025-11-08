package dev.owlmajin.flagforge.server.persistence.config

import dev.owlmajin.flagforge.server.common.kafka.config.CommonKafkaConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Import(CommonKafkaConfiguration::class)
@Configuration
@ComponentScan("dev.owlmajin.flagforge.server.persistence")
class PersistenceConfiguration

