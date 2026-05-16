package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.simulation.SimulationRepository;
import com.willembergfilho.ifinance.domain.simulation.SimulationSnapshot;
import com.willembergfilho.ifinance.domain.simulation.SnapshotRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
public class CreateSnapshotUseCase {

    private final SimulationRepository simulationRepository;
    private final SnapshotRepository snapshotRepository;

    public CreateSnapshotUseCase(SimulationRepository simulationRepository,
                                 SnapshotRepository snapshotRepository) {
        this.simulationRepository = simulationRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional
    public SimulationSnapshot execute(UUID userId, UUID simulationId) {
        var simulation = simulationRepository.findById(simulationId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new SimulationNotFoundException(simulationId));

        SimulationSnapshot snapshot = new SimulationSnapshot(
                UUID.randomUUID(),
                simulationId,
                simulation.getParameters(),
                Instant.now()
        );

        SimulationSnapshot saved = snapshotRepository.save(snapshot);
        log.info("Snapshot created: id={}, simulationId={}", saved.id(), simulationId);
        return saved;
    }
}
