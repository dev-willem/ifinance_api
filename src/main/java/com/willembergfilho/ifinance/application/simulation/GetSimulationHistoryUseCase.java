package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.simulation.Simulation;
import com.willembergfilho.ifinance.domain.simulation.SimulationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class GetSimulationHistoryUseCase {

    private final SimulationRepository simulationRepository;

    public GetSimulationHistoryUseCase(SimulationRepository simulationRepository) {
        this.simulationRepository = simulationRepository;
    }

    public record HistoryPage(List<Simulation> simulations, long total, int page, int size) {}

    public HistoryPage execute(UUID userId, int page, int size) {
        List<Simulation> simulations = simulationRepository.findByUserId(userId, page, size);
        long total = simulationRepository.countByUserId(userId);
        return new HistoryPage(simulations, total, page, size);
    }
}
