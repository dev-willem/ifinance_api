package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findBySubjectId(String subjectId);

    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);

    /** Locks the admin rows so concurrent role changes cannot leave the system without an admin. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<UserEntity> findByRole(UserEntity.Role role);

    @Query("SELECT u.role, COUNT(u) FROM UserEntity u GROUP BY u.role")
    List<Object[]> countGroupedByRole();

    @Query("SELECT u.provider, COUNT(u) FROM UserEntity u GROUP BY u.provider")
    List<Object[]> countGroupedByProvider();
}
