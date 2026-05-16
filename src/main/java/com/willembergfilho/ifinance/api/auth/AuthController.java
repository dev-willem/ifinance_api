package com.willembergfilho.ifinance.api.auth;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.security.UserContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
public class AuthController {

    private final UserContextHolder userContextHolder;
    private final UserJpaRepository userRepository;

    public AuthController(
            UserContextHolder userContextHolder,
            UserJpaRepository userRepository
    ) {
        this.userContextHolder = userContextHolder;
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public UserDto me() {

        UUID userId = userContextHolder.currentUserId();

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found"));

        return new UserDto(
                user.getId(),
                user.getEmail(),
                user.getName()
        );
    }

    public record UserDto(
            UUID id,
            String email,
            String name
    ) {}
}