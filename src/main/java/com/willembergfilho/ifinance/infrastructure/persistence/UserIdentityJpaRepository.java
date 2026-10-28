package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.UserIdentityEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserIdentityJpaRepository extends JpaRepository<UserIdentityEntity, String> {
}
