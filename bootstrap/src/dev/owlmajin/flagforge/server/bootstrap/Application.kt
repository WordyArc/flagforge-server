package dev.owlmajin.flagforge.server.bootstrap

import dev.owlmajin.flagforge.server.control.api.config.ControlApiConfiguration
import dev.owlmajin.flagforge.server.processor.config.ProcessorConfiguration
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.cloud.gateway.mvc.config.ProxyResponseAutoConfiguration
import org.springframework.context.annotation.Import

@Import(
    ControlApiConfiguration::class,
    ProcessorConfiguration::class,
)
@SpringBootApplication(
    exclude = [
        ProxyResponseAutoConfiguration::class
    ]
)
//@SpringBootApplication(scanBasePackages = ["dev.owlmajin.flagforge.server"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
