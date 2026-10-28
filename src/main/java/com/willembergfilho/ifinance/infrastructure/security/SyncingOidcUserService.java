package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Map;

/** OIDC logins (Google): syncs the user and returns a principal carrying the internal id and role. */
@Component
public class SyncingOidcUserService extends OidcUserService {

    private final UserSyncService userSyncService;

    public SyncingOidcUserService(UserSyncService userSyncService) {
        this.userSyncService = userSyncService;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest request) throws OAuth2AuthenticationException {
        OidcUser oidc = super.loadUser(request);
        UserEntity user = userSyncService.sync(OAuthIdentity.fromOidc(
                request.getClientRegistration().getRegistrationId(), oidc.getClaims()));

        Map<String, Object> userInfoClaims = oidc.getUserInfo() != null ? oidc.getUserInfo().getClaims() : Map.of();
        String nameKey = request.getClientRegistration().getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        return new DefaultOidcUser(
                UserSyncService.authoritiesWithRole(oidc.getAuthorities(), user),
                oidc.getIdToken(),
                new OidcUserInfo(UserSyncService.attributesWithUser(userInfoClaims, user)),
                StringUtils.hasText(nameKey) ? nameKey : IdTokenClaimNames.SUB);
    }
}
