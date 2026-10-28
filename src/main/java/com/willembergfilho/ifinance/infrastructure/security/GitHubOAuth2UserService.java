package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/** Plain OAuth2 (non-OIDC) logins. Only GitHub uses this flow; Google goes through {@link SyncingOidcUserService}. */
@Slf4j
@Component
public class GitHubOAuth2UserService extends DefaultOAuth2UserService {

    static final String GITHUB_UNAVAILABLE = "github_unavailable";

    private static final String EMAILS_URI = "https://api.github.com/user/emails";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final UserSyncService userSyncService;
    private final RestClient restClient;

    @Autowired
    public GitHubOAuth2UserService(UserSyncService userSyncService) {
        this(userSyncService, RestClient.builder().requestFactory(timeoutRequestFactory()).build());
    }

    GitHubOAuth2UserService(UserSyncService userSyncService, RestClient restClient) {
        this.userSyncService = userSyncService;
        this.restClient = restClient;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        String registrationId = request.getClientRegistration().getRegistrationId();
        if (!"github".equals(registrationId)) {
            throw new OAuth2AuthenticationException(new OAuth2Error("unsupported_provider"));
        }
        return syncedPrincipal(request, super.loadUser(request));
    }

    OAuth2User syncedPrincipal(OAuth2UserRequest request, OAuth2User github) {
        List<Map<String, Object>> emails = fetchEmails(request.getAccessToken().getTokenValue());
        UserEntity user;
        try {
            user = userSyncService.sync(OAuthIdentity.fromGitHub(github.getAttributes(), emails != null ? emails : List.of()));
        } catch (OAuth2AuthenticationException e) {
            // Without the e-mail list a new user is refused because GitHub is unreachable, not for an unverified e-mail.
            if (emails == null && UserSyncService.EMAIL_NOT_VERIFIED.equals(e.getError().getErrorCode())) {
                throw new OAuth2AuthenticationException(new OAuth2Error(GITHUB_UNAVAILABLE,
                        "Não foi possível consultar o GitHub. Tente novamente em instantes.", null), e);
            }
            throw e;
        }
        String nameKey = request.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOAuth2User(
                UserSyncService.authoritiesWithRole(github.getAuthorities(), user),
                UserSyncService.attributesWithUser(github.getAttributes(), user),
                nameKey);
    }

    /** Null when GitHub can't be reached: an already-known GitHub user still logs in, a new one gets github_unavailable. */
    private List<Map<String, Object>> fetchEmails(String accessToken) {
        try {
            List<Map<String, Object>> emails = restClient.get()
                    .uri(EMAILS_URI)
                    .headers(h -> h.setBearerAuth(accessToken))
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return emails != null ? emails : List.of();
        } catch (RestClientException e) {
            log.warn("Could not fetch GitHub e-mails: {}", e.getMessage());
            return null;
        }
    }

    // Bounded so a stalled api.github.com can't pin the servlet thread handling the OAuth2 callback.
    private static SimpleClientHttpRequestFactory timeoutRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(TIMEOUT);
        factory.setReadTimeout(TIMEOUT);
        return factory;
    }
}
