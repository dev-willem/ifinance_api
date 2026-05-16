package com.willembergfilho.ifinance.infrastructure.persistence;

import com.willembergfilho.ifinance.infrastructure.persistence.entity.SimulationSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SimulationSnapshotJpaRepository extends JpaRepository<SimulationSnapshotEntity, UUID> {
    List<SimulationSnapshotEntity> findBySimulationIdOrderBySnapshotAtAsc(UUID simulationId);
}
