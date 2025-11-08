package dev.owlmajin.flagforge.server.control.api.config

import dev.owlmajin.flagforge.server.common.CommonConfiguration
import dev.owlmajin.flagforge.server.common.kafka.config.CommonKafkaConfiguration
import dev.owlmajin.flagforge.server.persistence.config.PersistenceConfiguration
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile

@Import(value = [
    CommonConfiguration::class,
    CommonKafkaConfiguration::class,
    PersistenceConfiguration::class,
])
@Profile("control-api")
@ComponentScan(basePackages = ["dev.owlmajin.flagforge.server.control.api"])
class ControlApiConfiguration