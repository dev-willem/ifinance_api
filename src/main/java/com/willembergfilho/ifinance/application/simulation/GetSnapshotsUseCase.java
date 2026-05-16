package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.simulation.SimulationRepository;
import com.willembergfilho.ifinance.domain.simulation.SimulationSnapshot;
import com.willembergfilho.ifinance.domain.simulation.SnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetSnapshotsUseCase {

    private final SimulationRepository simulationRepository;
    private final SnapshotRepository snapshotRepository;

    public GetSnapshotsUseCase(SimulationRepository simulationRepository,
                               SnapshotRepository snapshotRepository) {
        this.simulationRepository = simulationRepository;
        this.snapshotRepository = snapshotRepository;
    }

    @Transactional(readOnly = true)
    public List<SimulationSnapshot> execute(UUID userId, UUID simulationId) {
        simulationRepository.findById(simulationId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new SimulationNotFoundException(simulationId));
        return snapshotRepository.findBySimulationId(simulationId);
    }
}
