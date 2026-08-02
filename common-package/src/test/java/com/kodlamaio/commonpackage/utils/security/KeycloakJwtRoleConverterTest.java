package com.kodlamaio.commonpackage.utils.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakJwtRoleConverterTest {

    private final KeycloakJwtRoleConverter converter = new KeycloakJwtRoleConverter();

    @Test
    void extractRoles_withMultipleRoles_returnsRolePrefixedAuthorities() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("admin", "user")))
                .build();

        var authorities = converter.extractRoles(jwt);

        assertThat(authorities)
                .containsExactlyInAnyOrder(
                        new SimpleGrantedAuthority("ROLE_admin"),
                        new SimpleGrantedAuthority("ROLE_user"));
        assertThat(authorities).hasSize(2);
    }

    @Test
    void extractRoles_withNoRealmAccessClaim_returnsEmptyList() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "someone")
                .build();

        var authorities = converter.extractRoles(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void extractRoles_withRealmAccessButNoRolesKey_returnsEmptyList() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of())
                .build();

        var authorities = converter.extractRoles(jwt);

        assertThat(authorities).isEmpty();
    }

    @Test
    void extractRoles_withSingleRole_returnsSingleAuthority() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of("roles", List.of("user")))
                .build();

        var authorities = converter.extractRoles(jwt);

        assertThat(authorities).containsExactly(new SimpleGrantedAuthority("ROLE_user"));
    }
}
