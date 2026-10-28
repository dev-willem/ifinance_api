package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.infrastructure.persistence.UserIdentityJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity.Role;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserIdentityEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UserSyncServiceTest {

    private UserJpaRepository userRepository;
    private UserIdentityJpaRepository identityRepository;
    private UserSyncService service;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserJpaRepository.class);
        identityRepository = mock(UserIdentityJpaRepository.class);
        when(userRepository.findBySubjectId(any())).thenReturn(Optional.empty());
        when(userRepository.findByEmailIgnoreCase(any())).thenReturn(Optional.empty());
        when(identityRepository.findById(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service = new UserSyncService(userRepository, identityRepository, " Boss@Example.com ,, other@x.com");
    }

    private static Map<String, Object> googleClaims(String sub, String email, boolean verified, String name) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", sub);
        claims.put("email", email);
        claims.put("email_verified", verified);
        claims.put("name", name);
        return claims;
    }

    private static Map<String, Object> githubUser(long id, String login, String name) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("id", id);
        attrs.put("login", login);
        attrs.put("name", name);
        attrs.put("email", null); // private e-mail: GitHub omits it from /user
        return attrs;
    }

    private static Map<String, Object> ghEmail(String email, boolean primary, boolean verified) {
        return Map.of("email", email, "primary", primary, "verified", verified);
    }

    private static UserEntity existingUser(String subject, String email, String provider) {
        return new UserEntity(UUID.randomUUID(), subject, email, "Existing", provider, null, null);
    }

    @Test
    void google_newUser_isCreatedWithRawSubAndUserRole() {
        UserEntity user = service.sync(OAuthIdentity.fromOidc("google",
                googleClaims("1234567890", "ana@gmail.com", true, "Ana")));

        assertThat(user.getSubjectId()).isEqualTo("1234567890");
        assertThat(user.getEmail()).isEqualTo("ana@gmail.com");
        assertThat(user.getName()).isEqualTo("Ana");
        assertThat(user.getProvider()).isEqualTo("google");
        assertThat(user.getRole()).isEqualTo(Role.USER);
        verify(identityRepository, never()).save(any());
    }

    @Test
    void google_existingUser_keepsStoredSubjectAndUpdatesName() {
        UserEntity existing = existingUser("legacy-sub", "ana@gmail.com", "google");
        when(userRepository.findBySubjectId("legacy-sub")).thenReturn(Optional.of(existing));

        UserEntity user = service.sync(OAuthIdentity.fromOidc("google",
                googleClaims("legacy-sub", "ana@gmail.com", false, "Ana Maria")));

        assertThat(user).isSameAs(existing);
        assertThat(user.getSubjectId()).isEqualTo("legacy-sub");
        assertThat(user.getName()).isEqualTo("Ana Maria");
    }

    @Test
    void github_privateEmail_usesPrimaryVerifiedEntryAndNamespacedSubject() {
        OAuthIdentity identity = OAuthIdentity.fromGitHub(githubUser(42L, "octocat", null), List.of(
                ghEmail("old@example.com", false, true),
                ghEmail("Octo@Example.com", true, true)));

        UserEntity user = service.sync(identity);

        assertThat(user.getSubjectId()).isEqualTo("github|42");
        assertThat(user.getEmail()).isEqualTo("octo@example.com");
        assertThat(user.getName()).isEqualTo("octocat"); // null "name" falls back to login
        assertThat(user.getProvider()).isEqualTo("github");
    }

    @Test
    void github_verifiedEmailOfExistingUser_linksIdentityToThatUser() {
        UserEntity googleUser = existingUser("google-sub", "ana@gmail.com", "google");
        when(userRepository.findByEmailIgnoreCase("ana@gmail.com")).thenReturn(Optional.of(googleUser));

        UserEntity user = service.sync(OAuthIdentity.fromGitHub(githubUser(7L, "ana", "Ana"),
                List.of(ghEmail("ANA@gmail.com", true, true))));

        assertThat(user).isSameAs(googleUser);
        assertThat(user.getSubjectId()).isEqualTo("google-sub");
        assertThat(user.getName()).isEqualTo("Existing"); // a linked provider never renames the account
        ArgumentCaptor<UserIdentityEntity> link = ArgumentCaptor.forClass(UserIdentityEntity.class);
        verify(identityRepository).save(link.capture());
        assertThat(link.getValue().getSubjectId()).isEqualTo("github|7");
        assertThat(link.getValue().getUserId()).isEqualTo(googleUser.getId());
        assertThat(link.getValue().getProvider()).isEqualTo("github");
    }

    @Test
    void linkedIdentity_resolvesToLinkedUserOnNextLogin() {
        UserEntity googleUser = existingUser("google-sub", "ana@gmail.com", "google");
        when(identityRepository.findById("github|7"))
                .thenReturn(Optional.of(new UserIdentityEntity("github|7", googleUser.getId(), "github")));
        when(userRepository.findById(googleUser.getId())).thenReturn(Optional.of(googleUser));

        // GitHub profile without a name: OAuthIdentity falls back to the login handle "ana".
        UserEntity user = service.sync(OAuthIdentity.fromGitHub(githubUser(7L, "ana", null), List.of()));

        assertThat(user).isSameAs(googleUser);
        assertThat(user.getName()).isEqualTo("Existing");
        verify(identityRepository, never()).save(any());
    }

    @Test
    void github_unverifiedPrimaryEmail_doesNotLinkNorCreate() {
        UserEntity victim = existingUser("google-sub", "victim@gmail.com", "google");
        when(userRepository.findByEmailIgnoreCase("victim@gmail.com")).thenReturn(Optional.of(victim));

        OAuthIdentity identity = OAuthIdentity.fromGitHub(githubUser(666L, "attacker", null),
                List.of(ghEmail("victim@gmail.com", true, false)));

        assertThat(identity.emailVerified()).isFalse();
        assertThatThrownBy(() -> service.sync(identity))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(e -> assertThat(((OAuth2AuthenticationException) e).getError().getErrorCode())
                        .isEqualTo(UserSyncService.EMAIL_NOT_VERIFIED));
        verify(identityRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    @Test
    void google_unverifiedEmail_doesNotLinkToExistingAccount() {
        UserEntity githubUser = existingUser("github|1", "bob@corp.com", "github");
        when(userRepository.findByEmailIgnoreCase("bob@corp.com")).thenReturn(Optional.of(githubUser));

        assertThatThrownBy(() -> service.sync(OAuthIdentity.fromOidc("google",
                googleClaims("g-attacker", "bob@corp.com", false, "Bob"))))
                .isInstanceOf(OAuth2AuthenticationException.class);
        verify(identityRepository, never()).save(any());
    }

    @Test
    void adminBootstrap_verifiedListedEmail_becomesAdmin_caseInsensitive() {
        UserEntity user = service.sync(OAuthIdentity.fromOidc("google",
                googleClaims("boss-sub", "boss@example.com", true, "Boss")));

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(UserSyncService.authoritiesWithRole(List.of(), user))
                .extracting(Object::toString).containsExactly("ROLE_ADMIN");
    }

    @Test
    void adminBootstrap_isReappliedAtEveryVerifiedLogin() {
        // Why the admin API refuses to demote listed e-mails (see UserAdministrationUseCase).
        UserEntity demoted = existingUser("boss-sub", "boss@example.com", "google");
        when(userRepository.findBySubjectId("boss-sub")).thenReturn(Optional.of(demoted));

        UserEntity user = service.sync(OAuthIdentity.fromOidc("google",
                googleClaims("boss-sub", "boss@example.com", true, "Boss")));

        assertThat(user.getRole()).isEqualTo(Role.ADMIN);
        assertThat(service.isBootstrapAdmin("BOSS@example.com")).isTrue();
        assertThat(service.isBootstrapAdmin("ana@gmail.com")).isFalse();
        assertThat(service.isBootstrapAdmin(null)).isFalse();
    }

    @Test
    void adminBootstrap_unverifiedListedEmail_staysUser() {
        UserEntity existing = existingUser("boss-sub", "boss@example.com", "google");
        when(userRepository.findBySubjectId("boss-sub")).thenReturn(Optional.of(existing));

        UserEntity user = service.sync(OAuthIdentity.fromOidc("google",
                googleClaims("boss-sub", "boss@example.com", false, "Boss")));

        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void adminBootstrap_emptyProperty_promotesNobody() {
        UserSyncService noAdmins = new UserSyncService(userRepository, identityRepository, "");

        UserEntity user = noAdmins.sync(OAuthIdentity.fromOidc("google",
                googleClaims("s", "boss@example.com", true, "Boss")));

        assertThat(user.getRole()).isEqualTo(Role.USER);
    }

    @Test
    void principalAttributes_carryInternalUserIdAndStoredSubject() {
        UserEntity user = existingUser("google-sub", "ana@gmail.com", "google");

        Map<String, Object> attrs = UserSyncService.attributesWithUser(Map.of("id", 7), user);

        assertThat(attrs).containsEntry(UserSyncService.USER_ID_ATTRIBUTE, user.getId().toString())
                .containsEntry(UserSyncService.SUBJECT_ATTRIBUTE, "google-sub")
                .containsEntry("id", 7);
    }
}
