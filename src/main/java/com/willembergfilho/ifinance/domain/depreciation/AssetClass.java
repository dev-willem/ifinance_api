package com.willembergfilho.ifinance.domain.depreciation;

import java.math.BigDecimal;

// IN RFB nº 1.700/2017, Anexo III (redação da IN RFB nº 1.881/2019) — tabela vigente em 2026
public enum AssetClass {
    BUILDINGS("Edificações", "Anexo III — Edificações", 25, "4"),
    INSTALLATIONS("Instalações", "Anexo III — Instalações", 10, "10"),
    MACHINERY_EQUIPMENT("Máquinas e equipamentos", "NCM Cap. 84 (regra geral)", 10, "10"),
    FURNITURE_FIXTURES("Móveis e utensílios", "NCM 9403", 10, "10"),
    PASSENGER_VEHICLES("Veículos de passageiros", "NCM 8703", 5, "20"),
    CARGO_VEHICLES("Veículos de carga", "NCM 8704", 4, "25"),
    COMPUTERS_PERIPHERALS("Computadores e periféricos", "NCM 8471", 5, "20"),
    TRACTORS("Tratores", "NCM 8701", 4, "25"),
    HAND_TOOLS("Ferramentas manuais", "NCM 8201 a 8207", 5, "20"),
    AIRCRAFT("Aeronaves", "NCM 8802", 10, "10"),
    SHIPS("Navios e embarcações de transporte", "NCM 8901", 20, "5");

    private final String description;
    private final String reference;
    private final int usefulLifeYears;
    private final BigDecimal annualRatePercent;

    AssetClass(String description, String reference, int usefulLifeYears, String annualRatePercent) {
        this.description = description;
        this.reference = reference;
        this.usefulLifeYears = usefulLifeYears;
        this.annualRatePercent = new BigDecimal(annualRatePercent);
    }

    public String description() { return description; }
    public String reference() { return reference; }
    public int usefulLifeYears() { return usefulLifeYears; }
    public BigDecimal annualRatePercent() { return annualRatePercent; }
}
