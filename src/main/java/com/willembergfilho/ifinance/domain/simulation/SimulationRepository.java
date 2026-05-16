package com.willembergfilho.ifinance.domain.simulation;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SimulationRepository {

    Simulation save(Simulation simulation);

    Optional<Simulation> findById(UUID id);

    List<Simulation> findByUserId(UUID userId, int page, int size);

    long countByUserId(UUID userId);

    List<Simulation> findAllById(List<UUID> ids);
}
