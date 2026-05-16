package com.willembergfilho.ifinance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "simulations")
public class SimulationEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column
    private String name;

    @Column(name = "amortization_system", nullable = false)
    private String amortizationSystem;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal principal;

    @Column(name = "interest_rate", nullable = false, precision = 20, scale = 10)
    private BigDecimal interestRate;

    @Column(name = "rate_type", nullable = false)
    private String rateType;

    @Column(nullable = false)
    private int term;

    @Column(nullable = false)
    private String periodicity;

    @Column(name = "cet_enabled", nullable = false)
    private boolean cetEnabled;

    @Column(name = "inflation_correction_enabled", nullable = false)
    private boolean inflationCorrectionEnabled;

    @Column(name = "inflation_index")
    private String inflationIndex;

    @Column(name = "total_paid", precision = 20, scale = 2)
    private BigDecimal totalPaid;

    @Column(name = "effective_cost_rate", precision = 20, scale = 10)
    private BigDecimal effectiveCostRate;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @OrderBy("periodNumber ASC")
    private List<InstallmentEntity> installments = new ArrayList<>();

    @OneToMany(mappedBy = "simulation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CetChargeEntity> cetCharges = new ArrayList<>();

    public SimulationEntity() {}

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAmortizationSystem() { return amortizationSystem; }
    public void setAmortizationSystem(String amortizationSystem) { this.amortizationSystem = amortizationSystem; }
    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }
    public BigDecimal getInterestRate() { return interestRate; }
    public void setInterestRate(BigDecimal interestRate) { this.interestRate = interestRate; }
    public String getRateType() { return rateType; }
    public void setRateType(String rateType) { this.rateType = rateType; }
    public int getTerm() { return term; }
    public void setTerm(int term) { this.term = term; }
    public String getPeriodicity() { return periodicity; }
    public void setPeriodicity(String periodicity) { this.periodicity = periodicity; }
    public boolean isCetEnabled() { return cetEnabled; }
    public void setCetEnabled(boolean cetEnabled) { this.cetEnabled = cetEnabled; }
    public boolean isInflationCorrectionEnabled() { return inflationCorrectionEnabled; }
    public void setInflationCorrectionEnabled(boolean inflationCorrectionEnabled) { this.inflationCorrectionEnabled = inflationCorrectionEnabled; }
    public String getInflationIndex() { return inflationIndex; }
    public void setInflationIndex(String inflationIndex) { this.inflationIndex = inflationIndex; }
    public BigDecimal getTotalPaid() { return totalPaid; }
    public void setTotalPaid(BigDecimal totalPaid) { this.totalPaid = totalPaid; }
    public BigDecimal getEffectiveCostRate() { return effectiveCostRate; }
    public void setEffectiveCostRate(BigDecimal effectiveCostRate) { this.effectiveCostRate = effectiveCostRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<InstallmentEntity> getInstallments() { return installments; }
    public void setInstallments(List<InstallmentEntity> installments) { this.installments = installments; }
    public List<CetChargeEntity> getCetCharges() { return cetCharges; }
    public void setCetCharges(List<CetChargeEntity> cetCharges) { this.cetCharges = cetCharges; }
}
