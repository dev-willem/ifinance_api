package com.willembergfilho.ifinance.domain.risk;

public enum Periodicity {
    DAILY(252),
    WEEKLY(52),
    MONTHLY(12),
    ANNUAL(1);

    private final int periodsPerYear;

    Periodicity(int periodsPerYear) {
        this.periodsPerYear = periodsPerYear;
    }

    public int periodsPerYear() {
        return periodsPerYear;
    }
}
