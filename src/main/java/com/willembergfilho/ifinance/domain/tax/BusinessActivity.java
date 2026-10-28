package com.willembergfilho.ifinance.domain.tax;

import java.math.BigDecimal;

/**
 * Presumption percentages: Lei 9.249/1995 arts. 15 (IRPJ) e 20 (CSLL).
 * Simples annexes: LC 123/2006 art. 18, §§ 5º-B a 5º-J.
 */
public enum BusinessActivity {
    COMMERCE(SimplesAnnex.I, "0.08", "0.12", TaxType.ICMS, false),
    INDUSTRY(SimplesAnnex.II, "0.08", "0.12", TaxType.ICMS, false),
    SERVICES(SimplesAnnex.III, "0.32", "0.32", TaxType.ISS, false),
    // Assumes services without supplied materials (e.g. construção civil with materials presumes 8%)
    SERVICES_ANNEX_IV(SimplesAnnex.IV, "0.32", "0.32", TaxType.ISS, false),
    // § 5º-I activities (e.g. engenharia, publicidade, TI): Anexo V, or Anexo III when Fator R >= 28%
    SERVICES_ANNEX_V(SimplesAnnex.V, "0.32", "0.32", TaxType.ISS, true),
    // Transporte municipal de passageiros: Anexo III (§ 5º-B, XIII); IRPJ 16% (art. 15, § 1º, II, a), CSLL 12%
    PASSENGER_TRANSPORT(SimplesAnnex.III, "0.16", "0.12", TaxType.ISS, false);

    private final SimplesAnnex simplesAnnex;
    private final BigDecimal irpjPresumption;
    private final BigDecimal csllPresumption;
    private final TaxType salesTax;
    private final boolean fatorR;

    BusinessActivity(SimplesAnnex simplesAnnex, String irpjPresumption, String csllPresumption,
                     TaxType salesTax, boolean fatorR) {
        this.simplesAnnex = simplesAnnex;
        this.irpjPresumption = new BigDecimal(irpjPresumption);
        this.csllPresumption = new BigDecimal(csllPresumption);
        this.salesTax = salesTax;
        this.fatorR = fatorR;
    }

    public SimplesAnnex simplesAnnex() { return simplesAnnex; }
    public BigDecimal irpjPresumption() { return irpjPresumption; }
    public BigDecimal csllPresumption() { return csllPresumption; }
    public TaxType salesTax() { return salesTax; }
    public boolean fatorR() { return fatorR; }
}
