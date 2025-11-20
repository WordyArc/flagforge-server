package dev.owlmajin.flagforge.server.cloud.gateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class CloudGateway

fun main(args: Array<String>) {
    runApplication<CloudGateway>(*args)
}
