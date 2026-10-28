package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.infrastructure.persistence.UserIdentityJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserIdentityEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserSyncService {

    /** Principal attributes carrying the internal user, so requests never re-resolve by provider subject. */
    public static final String USER_ID_ATTRIBUTE = "ifinance_user_id";
    public static final String SUBJECT_ATTRIBUTE = "ifinance_subject_id";

    static final String EMAIL_NOT_VERIFIED = "email_not_verified";

    private final UserJpaRepository userRepository;
    private final UserIdentityJpaRepository identityRepository;
    private final Set<String> adminEmails;

    public UserSyncService(UserJpaRepository userRepository,
                           UserIdentityJpaRepository identityRepository,
                           @Value("${app.security.admin-emails:}") String adminEmails) {
        this.userRepository = userRepository;
        this.identityRepository = identityRepository;
        this.adminEmails = Arrays.stream(adminEmails.split(","))
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * Resolves the login to a user: own subject, then a linked identity, then (verified e-mail only)
     * link to the account owning that e-mail, else create. An unverified e-mail never links nor creates,
     * so it can't take over an account or squat the UNIQUE e-mail of a future one.
     */
    @Transactional
    public UserEntity sync(OAuthIdentity identity) {
        Optional<UserEntity> primary = userRepository.findBySubjectId(identity.subjectId());
        UserEntity user = primary
                .or(() -> identityRepository.findById(identity.subjectId())
                        .flatMap(link -> userRepository.findById(link.getUserId())))
                .orElseGet(() -> linkOrCreate(identity));

        // Only the account's own provider renames it, so a linked login (e.g. a GitHub handle) doesn't flip the name.
        if (primary.isPresent() && identity.name() != null && !identity.name().isBlank()) {
            user.setName(identity.name());
        }
        if (identity.emailVerified() && isBootstrapAdmin(identity.email())) {
            user.setRole(UserEntity.Role.ADMIN);
        }
        return userRepository.save(user);
    }

    /** Listed in app.security.admin-emails: promoted again at every verified login, so demoting it wouldn't stick. */
    public boolean isBootstrapAdmin(String email) {
        return email != null && adminEmails.contains(email.toLowerCase(Locale.ROOT));
    }

    private UserEntity linkOrCreate(OAuthIdentity identity) {
        if (!identity.emailVerified()) {
            throw new OAuth2AuthenticationException(new OAuth2Error(EMAIL_NOT_VERIFIED,
                    "Sua conta precisa de um e-mail verificado no provedor para entrar.", null));
        }
        String email = identity.email().toLowerCase(Locale.ROOT);
        return userRepository.findByEmailIgnoreCase(email)
                .map(existing -> {
                    identityRepository.save(new UserIdentityEntity(
                            identity.subjectId(), existing.getId(), identity.provider()));
                    return existing;
                })
                .orElseGet(() -> new UserEntity(UUID.randomUUID(), identity.subjectId(), email,
                        identity.name() != null ? identity.name() : email, identity.provider(), null, null));
    }

    static Map<String, Object> attributesWithUser(Map<String, Object> attributes, UserEntity user) {
        Map<String, Object> merged = new HashMap<>(attributes);
        merged.put(USER_ID_ATTRIBUTE, user.getId().toString());
        merged.put(SUBJECT_ATTRIBUTE, user.getSubjectId());
        return merged;
    }

    static List<GrantedAuthority> authoritiesWithRole(Collection<? extends GrantedAuthority> base, UserEntity user) {
        List<GrantedAuthority> authorities = new ArrayList<>(base);
        authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
        return authorities;
    }
}
