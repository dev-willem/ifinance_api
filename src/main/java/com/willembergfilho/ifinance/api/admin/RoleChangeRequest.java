package com.willembergfilho.ifinance.api.admin;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import jakarta.validation.constraints.NotNull;

public record RoleChangeRequest(
        @NotNull(message = "O perfil é obrigatório (USER ou ADMIN).")
        UserEntity.Role role
) {}
