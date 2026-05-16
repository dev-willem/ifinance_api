package com.willembergfilho.ifinance.domain.simulation;

import java.time.Instant;
import java.util.UUID;

public class Simulation {

    private UUID id;
    private UUID userId;
    private SimulationParameters parameters;
    private AmortizationSchedule schedule;
    private SimulationStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Simulation(UUID userId, SimulationParameters parameters) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.parameters = parameters;
        this.status = SimulationStatus.DRAFT;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    // Used when reconstituting from persistence
    public Simulation(UUID id, UUID userId, SimulationParameters parameters,
                      AmortizationSchedule schedule, SimulationStatus status,
                      Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.parameters = parameters;
        this.schedule = schedule;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void applySchedule(AmortizationSchedule schedule) {
        this.schedule = schedule;
        this.status = SimulationStatus.CALCULATED;
        this.updatedAt = Instant.now();
    }

    public void markExported() {
        this.status = SimulationStatus.EXPORTED;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public SimulationParameters getParameters() { return parameters; }
    public AmortizationSchedule getSchedule() { return schedule; }
    public SimulationStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
