package com.kodlamaio.rentalservice.api.clients;

import feign.RequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

import java.util.Optional;

/**
 * Feign configuration for {@link PaymentClient} only, referenced via
 * {@code @FeignClient(configuration = ...)}.
 *
 * Intentionally carries NO class-level annotation. Component-scanning this class would register the
 * interceptor in the parent application context, from which every Feign client inherits it - including
 * CarClient, whose inventory-service endpoints are permitAll and work today with Keycloak stopped.
 * That failure is silent: the application still starts and still works while Keycloak is up.
 */
@Slf4j
public class PaymentClientTokenConfiguration {

    private static final String REGISTRATION_ID = "keycloak";
    // Constant, never the current user: this interceptor also runs on the SagaRecoveryScheduler thread,
    // where there is no authenticated principal at all.
    private static final String PRINCIPAL = "rental-service";

    @Bean
    public OAuth2AuthorizedClientManager paymentServiceAuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {
        var provider = OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build();
        // AuthorizedClientService- rather than Default-: the Default implementation resolves the token
        // from the current servlet request, which does not exist on the recovery scheduler's thread.
        var manager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);
        manager.setAuthorizedClientProvider(provider);

        return manager;
    }

    // Takes the manager as a parameter rather than calling the @Bean method above: without a
    // @Configuration annotation this class runs in lite mode, where a direct call would build a
    // second, unmanaged manager with its own token cache.
    @Bean
    public RequestInterceptor paymentServiceTokenInterceptor(OAuth2AuthorizedClientManager manager) {
        return template -> template.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken(manager));
    }

    private String accessToken(OAuth2AuthorizedClientManager manager) {
        var request = OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRATION_ID)
                .principal(PRINCIPAL)
                .build();
        try {
            // authorize() returns null when the registration's grant type is not exactly
            // client_credentials - a typo in the external config passes startup and fails only here.
            return Optional.ofNullable(manager.authorize(request))
                    .orElseThrow(() -> new IllegalStateException(
                            "No authorized client for registration '" + REGISTRATION_ID
                                    + "'; check that its authorization-grant-type is client_credentials"))
                    .getAccessToken()
                    .getTokenValue();
        } catch (RuntimeException exception) {
            // Logged rather than left to the handler: the circuit breaker replaces this exception with
            // PaymentClientFallback's "PAYMENT DOWN", so the real cause is destroyed one frame up.
            log.error("Could not obtain the service-account token for registration '{}'; the payment-service"
                    + " call will surface as PAYMENT DOWN: {}", REGISTRATION_ID, exception.getMessage());
            throw exception;
        }
    }
}
