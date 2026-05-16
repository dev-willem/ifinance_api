package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.SimulationEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SimulationJpaRepository extends JpaRepository<SimulationEntity, UUID> {

    Page<SimulationEntity> findByUserId(UUID userId, Pageable pageable);

    long countByUserId(UUID userId);

    @Modifying
    @Query("UPDATE SimulationEntity s SET s.status = :status WHERE s.id = :id")
    void updateStatus(@Param("id") UUID id, @Param("status") String status);
}
