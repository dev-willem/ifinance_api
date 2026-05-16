package com.willembergfilho.ifinance.domain.investment;

import java.time.Instant;
import java.util.UUID;

public class Investment {

    private final UUID id;
    private final UUID userId;
    private final InvestmentParameters parameters;
    private InvestmentResult result;
    private final InvestmentStatus status = InvestmentStatus.CALCULATED;
    private Instant createdAt;
    private Instant updatedAt;

    public Investment(UUID id, UUID userId, InvestmentParameters parameters) {
        this.id = id;
        this.userId = userId;
        this.parameters = parameters;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public Investment(UUID id, UUID userId, InvestmentParameters parameters,
                      InvestmentResult result, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.userId = userId;
        this.parameters = parameters;
        this.result = result;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void applyResult(InvestmentResult result) {
        this.result = result;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public InvestmentParameters getParameters() { return parameters; }
    public InvestmentResult getResult() { return result; }
    public InvestmentStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
