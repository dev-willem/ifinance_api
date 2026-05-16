package com.willembergfilho.ifinance.infrastructure.persistence.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "index_rates",
        uniqueConstraints = @UniqueConstraint(columnNames = {"index_code", "reference_date"}))
public class IndexRateEntity {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "index_code", nullable = false)
    private String indexCode;

    @Column(name = "reference_date", nullable = false)
    private LocalDate referenceDate;

    @Column(nullable = false, precision = 20, scale = 10)
    private BigDecimal rate;

    @Column
    private String source;

    @Column(name = "fetched_at")
    private Instant fetchedAt;

    public IndexRateEntity() {}

    @PrePersist
    void onCreate() {
        if (id == null) id = UUID.randomUUID();
        if (fetchedAt == null) fetchedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getIndexCode() { return indexCode; }
    public void setIndexCode(String indexCode) { this.indexCode = indexCode; }
    public LocalDate getReferenceDate() { return referenceDate; }
    public void setReferenceDate(LocalDate referenceDate) { this.referenceDate = referenceDate; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public Instant getFetchedAt() { return fetchedAt; }
    public void setFetchedAt(Instant fetchedAt) { this.fetchedAt = fetchedAt; }
}
