package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.infrastructure.persistence.UserIdentityJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GitHubOAuth2UserServiceTest {

    private static final String EMAILS_URI = "https://api.github.com/user/emails";

    private UserJpaRepository userRepository;
    private MockRestServiceServer github;
    private GitHubOAuth2UserService service;

    private final OAuth2UserRequest request = new OAuth2UserRequest(
            CommonOAuth2Provider.GITHUB.getBuilder("github").clientId("id").clientSecret("s").build(),
            new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, "gh-token",
                    Instant.now(), Instant.now().plusSeconds(60)));

    private final OAuth2User githubUser = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("OAUTH2_USER")),
            Map.of("id", 42, "login", "octocat"), "id");

    @BeforeEach
    void setUp() {
        userRepository = mock(UserJpaRepository.class);
        UserIdentityJpaRepository identityRepository = mock(UserIdentityJpaRepository.class);
        when(userRepository.findBySubjectId(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(identityRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RestClient.Builder builder = RestClient.builder();
        github = MockRestServiceServer.bindTo(builder).build();
        service = new GitHubOAuth2UserService(
                new UserSyncService(userRepository, identityRepository, ""), builder.build());
    }

    @Test
    void newUser_privateEmail_isFetchedWithAccessTokenAndCreatesUser() {
        github.expect(requestTo(EMAILS_URI))
                .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer gh-token"))
                .andRespond(withSuccess("""
                        [{"email":"old@example.com","primary":false,"verified":true},
                         {"email":"octo@example.com","primary":true,"verified":true}]
                        """, MediaType.APPLICATION_JSON));

        OAuth2User principal = service.syncedPrincipal(request, githubUser);

        github.verify();
        assertThat(principal.<String>getAttribute(UserSyncService.SUBJECT_ATTRIBUTE)).isEqualTo("github|42");
        assertThat(principal.getAuthorities()).extracting(Object::toString).contains("ROLE_USER");
        verify(userRepository).save(argThat(u -> "octo@example.com".equals(u.getEmail())));
    }

    @Test
    void newUser_gitHubUnreachable_isRefusedAsUnavailableNotAsUnverifiedEmail() {
        github.expect(requestTo(EMAILS_URI)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> service.syncedPrincipal(request, githubUser))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(GitHubOAuth2UserService.GITHUB_UNAVAILABLE));
        verify(userRepository, never()).save(any());
    }

    @Test
    void knownUser_gitHubUnreachable_stillLogsIn() {
        UserEntity known = new UserEntity(UUID.randomUUID(), "github|42", "octo@example.com", "Octo", "github", null, null);
        when(userRepository.findBySubjectId("github|42")).thenReturn(Optional.of(known));
        github.expect(requestTo(EMAILS_URI)).andRespond(withException(new SocketTimeoutException("Read timed out")));

        OAuth2User principal = service.syncedPrincipal(request, githubUser);

        assertThat(principal.<String>getAttribute(UserSyncService.USER_ID_ATTRIBUTE)).isEqualTo(known.getId().toString());
    }
}
