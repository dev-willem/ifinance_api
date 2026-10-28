package com.willembergfilho.ifinance.api.admin;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String name,
        String provider,
        UserEntity.Role role,
        Instant createdAt,
        @Schema(description = "E-mail listed in APP_ADMIN_EMAILS: re-promoted at every login, so it can't be demoted")
        boolean bootstrapAdmin
) {
    public static AdminUserResponse from(UserEntity user, boolean bootstrapAdmin) {
        return new AdminUserResponse(user.getId(), user.getEmail(), user.getName(),
                user.getProvider(), user.getRole(), user.getCreatedAt(), bootstrapAdmin);
    }
}
