package dev.owlmajin.flagforge.server.bootstrap

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication(scanBasePackages = ["dev.owlmajin.flagforge.server"])
class Application

fun main(args: Array<String>) {
    runApplication<Application>(*args)
}
