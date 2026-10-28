package com.willembergfilho.ifinance.domain.labor;

import com.willembergfilho.ifinance.domain.labor.LaborTaxTable.InssBracket;
import com.willembergfilho.ifinance.domain.labor.LaborTaxTable.IrrfBracket;
import com.willembergfilho.ifinance.domain.labor.LaborTaxTable.IrrfReduction;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Official payroll tables by year. Add a new entry each January when the new portaria/lei is published. */
public final class LaborTaxTables {

    // IRRF monthly table in force since May/2025 — Lei 15.191/2025 (MP 1.294/2025); unchanged for 2026
    // (gov.br/receitafederal "Tabelas 2026"). Dependent R$ 189,59; simplified discount R$ 607,20.
    private static final List<IrrfBracket> IRRF_SINCE_MAY_2025 = List.of(
            new IrrfBracket(bd("2428.80"), bd("0"), bd("0")),
            new IrrfBracket(bd("2826.65"), bd("0.075"), bd("182.16")),
            new IrrfBracket(bd("3751.05"), bd("0.15"), bd("394.16")),
            new IrrfBracket(bd("4664.68"), bd("0.225"), bd("675.49")),
            new IrrfBracket(null, bd("0.275"), bd("908.73"))
    );

    private static final TreeMap<Integer, LaborTaxTable> TABLES = new TreeMap<>(Map.of(
            // shortcut: Jan–Apr/2025 used the previous IRRF table (exempt up to R$ 2.259,20); key by YearMonth if those months matter.
            2025, new LaborTaxTable(2025,
                    bd("1518.00"), // Decreto 12.342/2024
                    // Portaria Interministerial MPS/MF 6/2025, Anexo II
                    List.of(new InssBracket(bd("1518.00"), bd("0.075")),
                            new InssBracket(bd("2793.88"), bd("0.09")),
                            new InssBracket(bd("4190.83"), bd("0.12")),
                            new InssBracket(bd("8157.41"), bd("0.14"))),
                    IRRF_SINCE_MAY_2025, bd("189.59"), bd("607.20"), null),
            2026, new LaborTaxTable(2026,
                    bd("1621.00"), // Decreto 12.797/2025
                    // Portaria Interministerial MPS/MF 13/2026 (ceiling R$ 8.475,55)
                    List.of(new InssBracket(bd("1621.00"), bd("0.075")),
                            new InssBracket(bd("2902.84"), bd("0.09")),
                            new InssBracket(bd("4354.27"), bd("0.12")),
                            new InssBracket(bd("8475.55"), bd("0.14"))),
                    IRRF_SINCE_MAY_2025, bd("189.59"), bd("607.20"),
                    // Lei 15.270/2025, art. 3º-A da Lei 9.250/1995 (from Jan/2026)
                    new IrrfReduction(bd("5000.00"), bd("312.89"), bd("7350.00"), bd("978.62"), bd("0.133145")))
    ));

    private LaborTaxTables() {}

    /** @param year null selects the latest available table */
    public static LaborTaxTable forYear(Integer year) {
        if (year == null) return TABLES.get(latestYear());
        LaborTaxTable table = TABLES.get(year);
        if (table == null) {
            throw new IllegalArgumentException("Tabelas trabalhistas indisponíveis para " + year
                    + ". Anos disponíveis: " + TABLES.keySet() + ".");
        }
        return table;
    }

    public static int latestYear() {
        return TABLES.lastKey();
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
