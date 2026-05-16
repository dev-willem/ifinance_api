package com.willembergfilho.ifinance.api.simulation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.willembergfilho.ifinance.domain.simulation.SimulationParameters;
import com.willembergfilho.ifinance.domain.simulation.SimulationSnapshot;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SnapshotResponse(
        UUID id,
        UUID simulationId,
        Instant snapshotAt,
        SimulationParameters parameters
) {
    public static SnapshotResponse from(SimulationSnapshot snapshot) {
        return new SnapshotResponse(
                snapshot.id(),
                snapshot.simulationId(),
                snapshot.snapshotAt(),
                snapshot.parameters()
        );
    }
}
