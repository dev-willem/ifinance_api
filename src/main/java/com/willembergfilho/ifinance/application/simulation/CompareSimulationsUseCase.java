package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.simulation.Simulation;
import com.willembergfilho.ifinance.domain.simulation.SimulationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

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

        // Ensure all simulations belong to the requesting user
        boolean unauthorized = simulations.stream()
                .anyMatch(s -> !s.getUserId().equals(userId));
        if (unauthorized) {
            throw new SecurityException("One or more simulations do not belong to the authenticated user.");
        }

        return simulations;
    }
}
