package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.simulation.Simulation;
import com.willembergfilho.ifinance.domain.simulation.SimulationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CompareSimulationsUseCase {

    private static final int MAX_COMPARISONS = 5;

    private final SimulationRepository simulationRepository;

    public CompareSimulationsUseCase(SimulationRepository simulationRepository) {
        this.simulationRepository = simulationRepository;
    }

    public List<Simulation> execute(UUID userId, List<UUID> simulationIds) {
        if (simulationIds == null || simulationIds.isEmpty()) {
            throw new IllegalArgumentException("At least one simulation ID must be provided.");
        }
        if (simulationIds.size() > MAX_COMPARISONS) {
            throw new IllegalArgumentException("Cannot compare more than " + MAX_COMPARISONS + " simulations at once.");
        }

        List<Simulation> simulations = simulationRepository.findAllById(simulationIds);

        List<Simulation> owned = simulations.stream()
                .filter(s -> s.getUserId().equals(userId))
                .toList();

        // 404 (not 403) for any ID that doesn't exist or isn't owned by the caller —
        // a distinct status would let callers enumerate other users' simulation IDs.
        if (owned.size() < simulationIds.size()) {
            Set<UUID> ownedIds = owned.stream().map(Simulation::getId).collect(Collectors.toSet());
            UUID missingOrForeign = simulationIds.stream()
                    .filter(id -> !ownedIds.contains(id))
                    .findFirst()
                    .orElseThrow();
            throw new SimulationNotFoundException(missingOrForeign);
        }

        return owned;
    }
}
