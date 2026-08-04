package com.kodlamaio.rentalservice.api.clients;

import feign.RequestTemplate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientAuthorizationException;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Component;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentClientTokenConfigurationTest {

    private static final String TOKEN_VALUE = "service-token";

    @Mock private OAuth2AuthorizedClientManager manager;
    @Mock private ClientRegistrationRepository clientRegistrationRepository;
    @Mock private OAuth2AuthorizedClientService authorizedClientService;

    private final PaymentClientTokenConfiguration configuration = new PaymentClientTokenConfiguration();

    private OAuth2AuthorizedClient authorizedClient() {
        var registration = ClientRegistration.withRegistrationId("keycloak")
                .clientId("rental-service-client")
                .clientSecret("secret")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .tokenUri("http://localhost:8081/token")
                .build();
        var token = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, TOKEN_VALUE,
                Instant.now(), Instant.now().plusSeconds(300));

        return new OAuth2AuthorizedClient(registration, "rental-service", token);
    }

    @Test
    void requestInterceptor_setsBearerAuthorizationHeaderFromTheServiceAccountToken() {
        when(manager.authorize(any())).thenReturn(authorizedClient());
        var template = new RequestTemplate();

        configuration.paymentServiceTokenInterceptor(manager).apply(template);

        assertThat(template.headers().get("Authorization")).containsExactly("Bearer " + TOKEN_VALUE);
    }

    @Test
    void requestInterceptor_authorizesWithTheKeycloakRegistrationAndAConstantPrincipal() {
        when(manager.authorize(any())).thenReturn(authorizedClient());

        configuration.paymentServiceTokenInterceptor(manager).apply(new RequestTemplate());

        var captor = ArgumentCaptor.forClass(OAuth2AuthorizeRequest.class);
        org.mockito.Mockito.verify(manager).authorize(captor.capture());
        assertThat(captor.getValue().getClientRegistrationId()).isEqualTo("keycloak");
        // A constant, not the current user: on the SagaRecoveryScheduler thread there is none.
        assertThat(captor.getValue().getPrincipal().getName()).isEqualTo("rental-service");
    }

    @Test
    void requestInterceptor_whenTheManagerReturnsNull_throwsIllegalStateExceptionNamingTheRegistration() {
        // authorize() returns null when the grant type is not exactly client_credentials.
        when(manager.authorize(any())).thenReturn(null);
        var template = new RequestTemplate();

        assertThatThrownBy(() -> configuration.paymentServiceTokenInterceptor(manager).apply(template))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("keycloak")
                .hasMessageContaining("client_credentials");

        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void requestInterceptor_whenTheTokenEndpointFails_rethrowsAndLeavesTheRequestUnauthenticated() {
        when(manager.authorize(any())).thenThrow(
                new ClientAuthorizationException(new OAuth2Error("invalid_client"), "keycloak"));
        var template = new RequestTemplate();

        assertThatThrownBy(() -> configuration.paymentServiceTokenInterceptor(manager).apply(template))
                .isInstanceOf(ClientAuthorizationException.class);

        // Never fall through unauthenticated - that would surface as a 401 from payment-service
        // instead of an error naming Keycloak.
        assertThat(template.headers()).doesNotContainKey("Authorization");
    }

    @Test
    void authorizedClientManager_isTheServletIndependentImplementation() {
        var built = configuration.paymentServiceAuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService);

        // DefaultOAuth2AuthorizedClientManager resolves the token from the current servlet request,
        // which does not exist on the recovery scheduler's thread - crash recovery and automatic
        // refund would be permanently broken.
        assertThat(built).isInstanceOf(AuthorizedClientServiceOAuth2AuthorizedClientManager.class);
    }

    @Test
    void tokenConfiguration_isNotComponentScannable() {
        // If this class ever becomes a @Configuration, its interceptor lands in the parent context and
        // silently applies to CarClient too, making Keycloak a hard dependency of permitAll endpoints.
        var annotations = MergedAnnotations.from(PaymentClientTokenConfiguration.class,
                MergedAnnotations.SearchStrategy.TYPE_HIERARCHY);

        assertThat(annotations.isPresent(Component.class)).isFalse();
    }

    @Test
    void paymentClient_isBoundToTheTokenConfigurationAndKeepsItsFallback() {
        var feignClient = PaymentClient.class.getAnnotation(FeignClient.class);

        assertThat(feignClient.configuration()).containsExactly(PaymentClientTokenConfiguration.class);
        assertThat(feignClient.fallback()).isEqualTo(PaymentClientFallback.class);
    }

    @Test
    void carClient_declaresNoFeignConfigurationSoItStaysKeycloakFree() {
        // inventory-service's endpoints are permitAll; adding a token buys nothing and would make
        // ensureCarIsAvailable fail whenever Keycloak is down.
        assertThat(CarClient.class.getAnnotation(FeignClient.class).configuration()).isEmpty();
    }
}
