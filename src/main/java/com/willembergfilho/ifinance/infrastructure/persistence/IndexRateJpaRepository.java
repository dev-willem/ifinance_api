package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.IndexRateEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IndexRateJpaRepository extends JpaRepository<IndexRateEntity, UUID> {

    Optional<IndexRateEntity> findByIndexCodeAndReferenceDate(String indexCode, LocalDate referenceDate);

    @Query("SELECT e FROM IndexRateEntity e WHERE e.indexCode = :indexCode ORDER BY e.referenceDate DESC LIMIT 1")
    Optional<IndexRateEntity> findLatestByIndexCode(String indexCode);

    List<IndexRateEntity> findByIndexCodeAndReferenceDateBetweenOrderByReferenceDateAsc(
            String indexCode, LocalDate from, LocalDate to);
}
