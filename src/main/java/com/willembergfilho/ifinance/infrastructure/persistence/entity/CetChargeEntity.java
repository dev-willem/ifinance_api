package com.willembergfilho.ifinance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "cet_charges")
public class CetChargeEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_id", nullable = false)
    private SimulationEntity simulation;

    @Column(nullable = false)
    private String description;

    @Column(name = "charge_type", nullable = false)
    private String chargeType;

    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal amount;

    @Column(name = "applies_on_period")
    private Integer appliesOnPeriod;

    protected CetChargeEntity() {}

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
    }

    public UUID getId() { return id; }
    public SimulationEntity getSimulation() { return simulation; }
    public void setSimulation(SimulationEntity simulation) { this.simulation = simulation; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getChargeType() { return chargeType; }
    public void setChargeType(String chargeType) { this.chargeType = chargeType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public Integer getAppliesOnPeriod() { return appliesOnPeriod; }
    public void setAppliesOnPeriod(Integer appliesOnPeriod) { this.appliesOnPeriod = appliesOnPeriod; }
}
