package com.willembergfilho.ifinance.api.admin;

import com.willembergfilho.ifinance.application.admin.UserAdministrationUseCase;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import com.willembergfilho.ifinance.infrastructure.security.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "admin")
@SecurityRequirement(name = "oauth2")
public class AdminController {

    private final UserAdministrationUseCase useCase;
    private final UserContextHolder userContext;

    public AdminController(UserAdministrationUseCase useCase, UserContextHolder userContext) {
        this.useCase = useCase;
        this.userContext = userContext;
    }

    @GetMapping("/users")
    @Operation(summary = "List users (paginated, newest first)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Users listed"),
            @ApiResponse(responseCode = "400", description = "Invalid page parameters"),
            @ApiResponse(responseCode = "403", description = "Not an administrator")
    })
    public ResponseEntity<UsersPageResponse> users(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Page<UserEntity> result = useCase.listUsers(userContext.currentUserId(), page, size);
        List<AdminUserResponse> items = result.getContent().stream().map(this::toResponse).toList();
        return ResponseEntity.ok(new UsersPageResponse(items, result.getTotalElements(), page, size));
    }

    @PatchMapping("/users/{id}/role")
    @Operation(summary = "Change a user's role (USER or ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role changed"),
            @ApiResponse(responseCode = "400", description = "Validation error, self-demotion or demoting an APP_ADMIN_EMAILS admin"),
            @ApiResponse(responseCode = "403", description = "Not an administrator"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    public ResponseEntity<AdminUserResponse> changeRole(@PathVariable UUID id,
                                                        @Valid @RequestBody RoleChangeRequest request) {
        return useCase.changeRole(userContext.currentUserId(), id, request.role())
                .map(user -> ResponseEntity.ok(toResponse(user)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/stats")
    @Operation(summary = "Platform usage statistics")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Statistics calculated"),
            @ApiResponse(responseCode = "403", description = "Not an administrator")
    })
    public ResponseEntity<AdminStatsResponse> stats() {
        return ResponseEntity.ok(AdminStatsResponse.from(useCase.stats(userContext.currentUserId())));
    }

    private AdminUserResponse toResponse(UserEntity user) {
        return AdminUserResponse.from(user, useCase.isBootstrapAdmin(user));
    }

    @Schema(name = "AdminUsersPageResponse")
    public record UsersPageResponse(List<AdminUserResponse> users, long total, int page, int size) {}
}
