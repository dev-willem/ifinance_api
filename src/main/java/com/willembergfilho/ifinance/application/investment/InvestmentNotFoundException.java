package com.willembergfilho.ifinance.application.investment;

import java.util.UUID;

public class InvestmentNotFoundException extends RuntimeException {

    public InvestmentNotFoundException(UUID id) {
        super("Investment not found: " + id);
    }
}
