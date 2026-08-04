package com.kodlamaio.commonpackage.security;

import com.kodlamaio.commonpackage.utils.security.KeycloakJwtRoleConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfig {
  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    var converter = new JwtAuthenticationConverter();
    converter.setJwtGrantedAuthoritiesConverter(new KeycloakJwtRoleConverter());

    http.cors()
        .and()
        .authorizeHttpRequests()
        .requestMatchers(
            "/api/filters",
            "/api/cars/check-car-available/**",
            "/api/payments/check",
            "/api/cars",
            "/api/cars/**",
            "/actuator/**")
        .permitAll()
        // Inter-service only: rental-service calls these with a Keycloak client_credentials token.
        // Must stay ahead of the /api/** rule below, which would otherwise let any logged-in user
        // move money directly. NOTE: matcher order is not unit-testable without a Spring context -
        // the curl checklist in the plan's Step 8 is the regression test of record.
        .requestMatchers(
            "/api/payments/process-rental-payment",
            "/api/payments/refund-rental-payment")
        .hasRole("service")
        .requestMatchers("/api/**")
        .hasAnyRole("user")
        .anyRequest()
        .authenticated()
        .and()
        .csrf()
        .disable()
        .oauth2ResourceServer()
        .jwt()
        .jwtAuthenticationConverter(converter);

    return http.build();
  }
}