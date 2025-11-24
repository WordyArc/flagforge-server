package dev.owlmajin.flagforge.server.cloud.gateway.config

import dev.owlmajin.oidc.authproxy.spring.security.JwtAuthProxyConverter
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
class SecurityConfiguration(private val jwtAuthProxyConverter: JwtAuthProxyConverter) {


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
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(
                        ReactiveJwtAuthenticationConverterAdapter(jwtAuthProxyConverter)
                    )
                }
            }
            .build()

}
