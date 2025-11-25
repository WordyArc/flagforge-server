package dev.owlmajin.flagforge.server.cloud.gateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
@EnableReactiveMethodSecurity
class SecurityConfiguration() {


    @Bean
    fun filterChain(http: ServerHttpSecurity): SecurityWebFilterChain =
        http
            .csrf { it.disable() }
            .authorizeExchange {
                it.pathMatchers("/actuator/**").permitAll()
                it.pathMatchers("/api/v1/control/**")
                    .hasAuthority("flagforge-cloud-gateway_api_admin")
                it.pathMatchers("/api/v1/evaluation/**")
                    .hasAnyAuthority("flagforge-cloud-gateway_api_user", "flagforge-cloud-gateway_api_admin")
                it.anyExchange().authenticated()
            }.build()

}
