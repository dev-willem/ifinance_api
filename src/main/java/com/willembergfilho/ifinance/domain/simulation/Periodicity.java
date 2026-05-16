package com.willembergfilho.ifinance.domain.simulation;

public enum Periodicity {
    MONTHLY(12),
    QUARTERLY(4),
    ANNUAL(1);

    private final int periodsPerYear;

    Periodicity(int periodsPerYear) {
        this.periodsPerYear = periodsPerYear;
    }

    public int getPeriodsPerYear() {
        return periodsPerYear;
    }
}
