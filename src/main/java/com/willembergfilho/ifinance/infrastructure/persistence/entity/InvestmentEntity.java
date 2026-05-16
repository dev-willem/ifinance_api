package com.willembergfilho.ifinance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "investments")
public class InvestmentEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column
    private String name;

    @Column(name = "investment_type", nullable = false)
    private String investmentType;

    @Column(name = "rate_basis", nullable = false)
    private String rateBasis;

    @Column(name = "rate_value", nullable = false, precision = 20, scale = 10)
    private BigDecimal rateValue;

    @Column(nullable = false, precision = 20, scale = 2)
    private BigDecimal principal;

    @Column(name = "term_days", nullable = false)
    private int termDays;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "is_tax_exempt", nullable = false)
    private boolean isTaxExempt;

    @Column(name = "gross_return", precision = 20, scale = 2)
    private BigDecimal grossReturn;

    @Column(name = "net_return", precision = 20, scale = 2)
    private BigDecimal netReturn;

    @Column(name = "ir_rate", precision = 20, scale = 10)
    private BigDecimal irRate;

    @Column(name = "ir_amount", precision = 20, scale = 2)
    private BigDecimal irAmount;

    @Column(name = "gross_annual_rate", precision = 20, scale = 10)
    private BigDecimal grossAnnualRate;

    @Column(name = "net_annual_rate", precision = 20, scale = 10)
    private BigDecimal netAnnualRate;

    @Column(name = "index_rate_used", precision = 20, scale = 10)
    private BigDecimal indexRateUsed;

    @Column(nullable = false)
    private String status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public InvestmentEntity() {}

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
    public String getInvestmentType() { return investmentType; }
    public void setInvestmentType(String investmentType) { this.investmentType = investmentType; }
    public String getRateBasis() { return rateBasis; }
    public void setRateBasis(String rateBasis) { this.rateBasis = rateBasis; }
    public BigDecimal getRateValue() { return rateValue; }
    public void setRateValue(BigDecimal rateValue) { this.rateValue = rateValue; }
    public BigDecimal getPrincipal() { return principal; }
    public void setPrincipal(BigDecimal principal) { this.principal = principal; }
    public int getTermDays() { return termDays; }
    public void setTermDays(int termDays) { this.termDays = termDays; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public boolean isTaxExempt() { return isTaxExempt; }
    public void setTaxExempt(boolean taxExempt) { this.isTaxExempt = taxExempt; }
    public BigDecimal getGrossReturn() { return grossReturn; }
    public void setGrossReturn(BigDecimal grossReturn) { this.grossReturn = grossReturn; }
    public BigDecimal getNetReturn() { return netReturn; }
    public void setNetReturn(BigDecimal netReturn) { this.netReturn = netReturn; }
    public BigDecimal getIrRate() { return irRate; }
    public void setIrRate(BigDecimal irRate) { this.irRate = irRate; }
    public BigDecimal getIrAmount() { return irAmount; }
    public void setIrAmount(BigDecimal irAmount) { this.irAmount = irAmount; }
    public BigDecimal getGrossAnnualRate() { return grossAnnualRate; }
    public void setGrossAnnualRate(BigDecimal grossAnnualRate) { this.grossAnnualRate = grossAnnualRate; }
    public BigDecimal getNetAnnualRate() { return netAnnualRate; }
    public void setNetAnnualRate(BigDecimal netAnnualRate) { this.netAnnualRate = netAnnualRate; }
    public BigDecimal getIndexRateUsed() { return indexRateUsed; }
    public void setIndexRateUsed(BigDecimal indexRateUsed) { this.indexRateUsed = indexRateUsed; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
