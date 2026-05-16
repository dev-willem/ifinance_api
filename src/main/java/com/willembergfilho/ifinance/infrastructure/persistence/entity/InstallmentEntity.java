package com.willembergfilho.ifinance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "installments",
        uniqueConstraints = @UniqueConstraint(columnNames = {"simulation_id", "period_number"}))
public class InstallmentEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "simulation_id", nullable = false)
    private SimulationEntity simulation;

    @Column(name = "period_number", nullable = false)
    private int periodNumber;

    @Column(name = "principal_balance_before", nullable = false, precision = 20, scale = 2)
    private BigDecimal principalBalanceBefore;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal amortization;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal interest;

    @Column(name = "additional_charges", nullable = false, precision = 20, scale = 2)
    private BigDecimal additionalCharges;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal total;

    @Column(name = "principal_balance_after", nullable = false, precision = 20, scale = 2)
    private BigDecimal principalBalanceAfter;

    @Column(name = "corrected_total", precision = 20, scale = 2)
    private BigDecimal correctedTotal;

    public InstallmentEntity() {}

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
    }

    public UUID getId() { return id; }
    public SimulationEntity getSimulation() { return simulation; }
    public void setSimulation(SimulationEntity simulation) { this.simulation = simulation; }
    public int getPeriodNumber() { return periodNumber; }
    public void setPeriodNumber(int periodNumber) { this.periodNumber = periodNumber; }
    public BigDecimal getPrincipalBalanceBefore() { return principalBalanceBefore; }
    public void setPrincipalBalanceBefore(BigDecimal principalBalanceBefore) { this.principalBalanceBefore = principalBalanceBefore; }
    public BigDecimal getAmortization() { return amortization; }
    public void setAmortization(BigDecimal amortization) { this.amortization = amortization; }
    public BigDecimal getInterest() { return interest; }
    public void setInterest(BigDecimal interest) { this.interest = interest; }
    public BigDecimal getAdditionalCharges() { return additionalCharges; }
    public void setAdditionalCharges(BigDecimal additionalCharges) { this.additionalCharges = additionalCharges; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getPrincipalBalanceAfter() { return principalBalanceAfter; }
    public void setPrincipalBalanceAfter(BigDecimal principalBalanceAfter) { this.principalBalanceAfter = principalBalanceAfter; }
    public BigDecimal getCorrectedTotal() { return correctedTotal; }
    public void setCorrectedTotal(BigDecimal correctedTotal) { this.correctedTotal = correctedTotal; }
}
