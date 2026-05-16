package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.UUID;

@Component
public class UserContextHolder {

    private final UserJpaRepository userRepository;
    private final Environment environment;

    public UserContextHolder(
            UserJpaRepository userRepository,
            Environment environment
    ) {
        this.userRepository = userRepository;
        this.environment = environment;
    }

    public UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return handleUnauthenticated();
        }

        Object principal = auth.getPrincipal();

        if (!(principal instanceof OAuth2User oAuth2User)) {
            return handleUnauthenticated();
        }

        String subjectId = oAuth2User.getAttribute("sub");

        return userRepository.findBySubjectId(subjectId)
                .map(UserEntity::getId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Authenticated user not found in database."
                        ));
    }

    public String currentUserSubjectId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth == null) {
            return handleUnauthenticatedSubject();
        }

        Object principal = auth.getPrincipal();

        if (!(principal instanceof OAuth2User oAuth2User)) {
            return handleUnauthenticatedSubject();
        }

        return oAuth2User.getAttribute("sub");
    }

    private UUID handleUnauthenticated() {
        if (isLocalProfileActive()) {
            return DevUserInitializer.DEV_USER_ID;
        }

        throw new IllegalStateException("No authenticated user.");
    }

    private String handleUnauthenticatedSubject() {
        if (isLocalProfileActive()) {
            return DevUserInitializer.DEV_SUBJECT_ID;
        }

        throw new IllegalStateException("No authenticated user.");
    }

    private boolean isLocalProfileActive() {
        return Arrays.asList(environment.getActiveProfiles())
                .contains("local");
    }
}