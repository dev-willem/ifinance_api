package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findBySubjectId(String subjectId);

    Optional<UserEntity> findByEmail(String email);
}
