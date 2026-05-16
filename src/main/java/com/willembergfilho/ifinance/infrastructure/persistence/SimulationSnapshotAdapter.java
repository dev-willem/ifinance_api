package com.willembergfilho.ifinance.infrastructure.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.willembergfilho.ifinance.domain.simulation.SimulationParameters;
import com.willembergfilho.ifinance.domain.simulation.SimulationSnapshot;
import com.willembergfilho.ifinance.domain.simulation.SnapshotRepository;
import com.willembergfilho.ifinance.infrastructure.persistence.entity.SimulationSnapshotEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class SimulationSnapshotAdapter implements SnapshotRepository {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .findAndRegisterModules()
            .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);

    private final SimulationSnapshotJpaRepository jpaRepository;

    public SimulationSnapshotAdapter(SimulationSnapshotJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public SimulationSnapshot save(SimulationSnapshot snapshot) {
        SimulationSnapshotEntity entity = SimulationSnapshotEntity.of(
                snapshot.simulationId(), serialize(snapshot.parameters()));
        SimulationSnapshotEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SimulationSnapshot> findBySimulationId(UUID simulationId) {
        return jpaRepository.findBySimulationIdOrderBySnapshotAtAsc(simulationId)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private SimulationSnapshot toDomain(SimulationSnapshotEntity entity) {
        return new SimulationSnapshot(
                entity.getId(),
                entity.getSimulationId(),
                deserialize(entity.getParametersSnapshot()),
                entity.getSnapshotAt()
        );
    }

    private String serialize(SimulationParameters parameters) {
        try {
            return MAPPER.writeValueAsString(parameters);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SimulationParameters", e);
        }
    }

    private SimulationParameters deserialize(String json) {
        try {
            return MAPPER.readValue(json, SimulationParameters.class);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize SimulationParameters", e);
        }
    }
}
