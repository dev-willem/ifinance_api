package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.infrastructure.persistence.UserIdentityJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SyncingOidcUserServiceTest {

    @Test
    void googlePrincipal_carriesInternalUserIdAndRole() {
        UserJpaRepository users = mock(UserJpaRepository.class);
        UserEntity existing = new UserEntity(UUID.randomUUID(), "g-123", "ana@gmail.com", "Ana", "google", null, null);
        existing.setRole(UserEntity.Role.ADMIN);
        when(users.findBySubjectId("g-123")).thenReturn(Optional.of(existing));
        when(users.save(any())).thenAnswer(inv -> inv.getArgument(0));
        SyncingOidcUserService service = new SyncingOidcUserService(
                new UserSyncService(users, mock(UserIdentityJpaRepository.class), ""));

        // No user-info URI: OidcUserService builds the user from the ID token alone (no HTTP call).
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId("id").clientSecret("s")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .scope("openid", "email", "profile")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userNameAttributeName("sub")
                .build();
        Instant now = Instant.now();
        OidcIdToken idToken = new OidcIdToken("token", now, now.plusSeconds(60),
                Map.of("sub", "g-123", "email", "ana@gmail.com", "email_verified", true, "name", "Ana"));
        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "at",
                now, now.plusSeconds(60), Set.of("openid", "email", "profile"));

        OidcUser principal = service.loadUser(new OidcUserRequest(google, accessToken, idToken));

        assertThat(principal.<String>getAttribute(UserSyncService.USER_ID_ATTRIBUTE)).isEqualTo(existing.getId().toString());
        assertThat(principal.<String>getAttribute(UserSyncService.SUBJECT_ATTRIBUTE)).isEqualTo("g-123");
        assertThat(principal.getName()).isEqualTo("g-123");
        assertThat(principal.getAuthorities()).extracting(Object::toString).contains("ROLE_ADMIN");
    }
}
