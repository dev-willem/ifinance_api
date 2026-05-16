package com.willembergfilho.ifinance.infrastructure.security;

import com.willembergfilho.ifinance.infrastructure.persistence.UserJpaRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Cria um usuário de desenvolvimento fixo na inicialização quando o perfil local está ativo.
 * Permite testar todos os endpoints sem autenticação OAuth2.
 */
@Component
@Profile("local")
public class DevUserInitializer implements ApplicationRunner {

    static final UUID DEV_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    static final String DEV_SUBJECT_ID = "dev-local-user";

    private final UserJpaRepository userRepository;

    public DevUserInitializer(UserJpaRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        userRepository.findBySubjectId(DEV_SUBJECT_ID).orElseGet(() -> {
            UserEntity dev = new UserEntity(
                    DEV_USER_ID, DEV_SUBJECT_ID,
                    "dev@ifinance.local", "Dev User", "local",
                    null, null);
            return userRepository.save(dev);
        });
    }
}
