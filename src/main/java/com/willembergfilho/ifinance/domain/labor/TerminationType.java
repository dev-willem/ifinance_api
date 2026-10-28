package com.willembergfilho.ifinance.domain.labor;

import java.math.BigDecimal;

/**
 * @param fgtsFineRate       multa rescisória on the FGTS balance (Lei 8.036/1990 art. 18 §1º; CLT art. 484-A I "b")
 * @param fgtsWithdrawalRate share of the FGTS balance the employee may withdraw (Lei 8.036/1990 art. 20 I, I-A, IX)
 */
public enum TerminationType {
    WITHOUT_CAUSE("0.40", "1.00"),
    RESIGNATION("0", "0"),
    WITH_CAUSE("0", "0"),
    MUTUAL_AGREEMENT("0.20", "0.80"),
    END_OF_FIXED_TERM("0", "1.00");

    private final BigDecimal fgtsFineRate;
    private final BigDecimal fgtsWithdrawalRate;

    TerminationType(String fgtsFineRate, String fgtsWithdrawalRate) {
        this.fgtsFineRate = new BigDecimal(fgtsFineRate);
        this.fgtsWithdrawalRate = new BigDecimal(fgtsWithdrawalRate);
    }

    public BigDecimal fgtsFineRate() {
        return fgtsFineRate;
    }

    public BigDecimal fgtsWithdrawalRate() {
        return fgtsWithdrawalRate;
    }
}
