package com.willembergfilho.ifinance.domain.investment;

public enum InvestmentType {
    CDB,
    LCI,
    LCA,
    DEBENTURE,
    TESOURO_SELIC,
    TESOURO_IPCA_PLUS,
    PRE_FIXADO;

    public boolean isTaxExempt() {
        return this == LCI || this == LCA;
    }
}
