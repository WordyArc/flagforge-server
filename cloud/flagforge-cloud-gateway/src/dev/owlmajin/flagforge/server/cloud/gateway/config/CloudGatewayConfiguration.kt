package dev.owlmajin.flagforge.server.cloud.gateway.config

import org.springframework.cloud.gateway.route.RouteLocator
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder
import org.springframework.cloud.gateway.route.builder.RouteLocatorDsl
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class CloudGatewayConfiguration {

    @Bean
    fun routes(builder: RouteLocatorBuilder): RouteLocator =
        RouteLocatorDsl(builder).apply {
            route(id = "control-api", uri = "http://host.docker.internal:8184") {
                path("/api/v1/control/**")
            }
            route(id = "evaluation-api", uri = "http://host.docker.internal:8185") {
                path("/api/v1/evaluation").filters {
                    it.addRequestHeader("x-cloud-gateway", "flagforge")
                }
            }
        }.build()

}