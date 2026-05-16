package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class UserSyncService {

    private final UserJpaRepository userRepository;

    public UserSyncService(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public UserEntity syncFromOAuth2(OAuth2User oAuth2User, String provider) {
        String subjectId = oAuth2User.getAttribute("sub");
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");

        return userRepository.findBySubjectId(subjectId)
                .map(existing -> {
                    existing.setName(name);
                    return userRepository.save(existing);
                })
                .orElseGet(() -> {
                    UserEntity newUser = new UserEntity(
                            UUID.randomUUID(), subjectId, email, name, provider, null, null);
                    return userRepository.save(newUser);
                });
    }
}
