package com.willembergfilho.ifinance.domain.simulation;

import java.util.List;
import java.util.UUID;

public interface SnapshotRepository {
    SimulationSnapshot save(SimulationSnapshot snapshot);
    List<SimulationSnapshot> findBySimulationId(UUID simulationId);
}
