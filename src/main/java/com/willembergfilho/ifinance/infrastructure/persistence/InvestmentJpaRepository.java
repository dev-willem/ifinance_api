package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.InvestmentEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface InvestmentJpaRepository extends JpaRepository<InvestmentEntity, UUID> {

    Page<InvestmentEntity> findByUserId(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    @Modifying
    @Query("UPDATE InvestmentEntity i SET i.status = :status WHERE i.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);
}
