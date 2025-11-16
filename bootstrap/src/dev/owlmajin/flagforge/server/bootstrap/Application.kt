package dev.owlmajin.flagforge.server.bootstrap

import dev.owlmajin.flagforge.server.control.api.config.ControlApiConfiguration
import dev.owlmajin.flagforge.server.evaluation.api.config.EvaluationApiConfiguration
import dev.owlmajin.flagforge.server.processor.config.ProcessorConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.mvc.config.ProxyResponseAutoConfiguration
import org.springframework.context.annotation.Import
import org.springframework.kafka.annotation.EnableKafkaStreams

@Import(
    ControlApiConfiguration::class,
    ProcessorConfiguration::class,
    EvaluationApiConfiguration::class,
)
@EnableKafkaStreams
@SpringBootApplication(
    //scanBasePackages = ["dev.owlmajin.flagforge.server"],
    exclude = [
        ProxyResponseAutoConfiguration::class
    ]
)
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
