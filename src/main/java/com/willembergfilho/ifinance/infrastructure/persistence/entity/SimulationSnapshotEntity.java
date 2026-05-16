package com.willembergfilho.ifinance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "simulation_snapshots")
public class SimulationSnapshotEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "simulation_id", nullable = false, columnDefinition = "uuid")
    private UUID simulationId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "parameters_snapshot", nullable = false, columnDefinition = "jsonb")
    private String parametersSnapshot;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;

    protected SimulationSnapshotEntity() {}

    public static SimulationSnapshotEntity of(UUID simulationId, String parametersSnapshot) {
        SimulationSnapshotEntity e = new SimulationSnapshotEntity();
        e.simulationId = simulationId;
        e.parametersSnapshot = parametersSnapshot;
        return e;
    }

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (snapshotAt == null) snapshotAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getSimulationId() { return simulationId; }
    public void setSimulationId(UUID simulationId) { this.simulationId = simulationId; }
    public String getParametersSnapshot() { return parametersSnapshot; }
    public void setParametersSnapshot(String parametersSnapshot) { this.parametersSnapshot = parametersSnapshot; }
    public Instant getSnapshotAt() { return snapshotAt; }
}
