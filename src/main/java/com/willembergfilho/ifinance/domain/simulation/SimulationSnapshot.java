package com.willembergfilho.ifinance.domain.simulation;

import java.time.Instant;
import java.util.UUID;

public record SimulationSnapshot(
        UUID id,
        UUID simulationId,
        SimulationParameters parameters,
        Instant snapshotAt
) {}
