package com.willembergfilho.ifinance.infrastructure.security;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Provider-neutral view of an OAuth2 login. {@code email} is only trustworthy for account
 * linking and admin bootstrap when {@code emailVerified} is true.
 */
public record OAuthIdentity(String provider, String subjectId, String email, boolean emailVerified, String name) {

    static final String GITHUB_SUBJECT_PREFIX = "github|";

    /** OIDC (Google): raw "sub" keeps existing users' stored subject_id unchanged. */
    public static OAuthIdentity fromOidc(String provider, Map<String, Object> claims) {
        String subject = (String) claims.get("sub");
        if (subject == null) throw new IllegalArgumentException("OIDC claims without 'sub'.");
        String email = (String) claims.get("email");
        boolean verified = email != null && isTrue(claims.get("email_verified"));
        return new OAuthIdentity(provider, subject, email, verified, firstNonBlank((String) claims.get("name"), email));
    }

    /**
     * GitHub: numeric "id" namespaced so it can never collide with a Google "sub". The profile
     * "email" is ignored; only the primary + verified entry from /user/emails is trusted.
     */
    public static OAuthIdentity fromGitHub(Map<String, Object> attributes, List<Map<String, Object>> emails) {
        Object id = attributes.get("id");
        if (id == null) throw new IllegalArgumentException("GitHub user without 'id'.");
        String email = emails.stream()
                .filter(e -> isTrue(e.get("primary")) && isTrue(e.get("verified")))
                .map(e -> (String) e.get("email"))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        String login = (String) attributes.get("login");
        return new OAuthIdentity("github", GITHUB_SUBJECT_PREFIX + id, email, email != null,
                firstNonBlank((String) attributes.get("name"), login));
    }

    private static boolean isTrue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    private static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }
}
