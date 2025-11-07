package dev.owlmajin.flagforge.server.common

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CommonConfiguration {

    @Bean
    fun idGenerator(): IdGenerator = DefaultIdGenerator()

}