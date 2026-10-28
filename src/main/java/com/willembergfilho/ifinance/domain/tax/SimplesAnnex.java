package com.willembergfilho.ifinance.domain.tax;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.willembergfilho.ifinance.domain.tax.TaxType.*;

/**
 * Simples Nacional tables — LC 123/2006 Anexos I a V, redação da LC 155/2016, vigentes de 2018 a 2026.
 * Rows: alíquota nominal (%), parcela a deduzir (R$), then the repartição (%) in the order of {@code taxes}.
 */
public enum SimplesAnnex {

    I(List.of(IRPJ, CSLL, COFINS, PIS_PASEP, CPP, ICMS), new String[][]{
            {"4.00", "0", "5.50", "3.50", "12.74", "2.76", "41.50", "34.00"},
            {"7.30", "5940", "5.50", "3.50", "12.74", "2.76", "41.50", "34.00"},
            {"9.50", "13860", "5.50", "3.50", "12.74", "2.76", "42.00", "33.50"},
            {"10.70", "22500", "5.50", "3.50", "12.74", "2.76", "42.00", "33.50"},
            {"14.30", "87300", "5.50", "3.50", "12.74", "2.76", "42.00", "33.50"},
            {"19.00", "378000", "13.50", "10.00", "28.27", "6.13", "42.10", "0"}
    }, null),

    II(List.of(IRPJ, CSLL, COFINS, PIS_PASEP, CPP, IPI, ICMS), new String[][]{
            {"4.50", "0", "5.50", "3.50", "11.51", "2.49", "37.50", "7.50", "32.00"},
            {"7.80", "5940", "5.50", "3.50", "11.51", "2.49", "37.50", "7.50", "32.00"},
            {"10.00", "13860", "5.50", "3.50", "11.51", "2.49", "37.50", "7.50", "32.00"},
            {"11.20", "22500", "5.50", "3.50", "11.51", "2.49", "37.50", "7.50", "32.00"},
            {"14.70", "85500", "5.50", "3.50", "11.51", "2.49", "37.50", "7.50", "32.00"},
            {"30.00", "720000", "8.50", "7.50", "20.96", "4.54", "23.50", "35.00", "0"}
    }, null),

    III(List.of(IRPJ, CSLL, COFINS, PIS_PASEP, CPP, ISS), new String[][]{
            {"6.00", "0", "4.00", "3.50", "12.82", "2.78", "43.40", "33.50"},
            {"11.20", "9360", "4.00", "3.50", "14.05", "3.05", "43.40", "32.00"},
            {"13.50", "17640", "4.00", "3.50", "13.64", "2.96", "43.40", "32.50"},
            {"16.00", "35640", "4.00", "3.50", "13.64", "2.96", "43.40", "32.50"},
            {"21.00", "125640", "4.00", "3.50", "12.82", "2.78", "43.40", "33.50"},
            {"33.00", "648000", "35.00", "15.00", "16.03", "3.47", "30.50", "0"}
    }, new String[]{"6.02", "5.26", "19.28", "4.18", "65.26"}),

    IV(List.of(IRPJ, CSLL, COFINS, PIS_PASEP, ISS), new String[][]{
            {"4.50", "0", "18.80", "15.20", "17.67", "3.83", "44.50"},
            {"9.00", "8100", "19.80", "15.20", "20.55", "4.45", "40.00"},
            {"10.20", "12420", "20.80", "15.20", "19.73", "4.27", "40.00"},
            {"14.00", "39780", "17.80", "19.20", "18.90", "4.10", "40.00"},
            {"22.00", "183780", "18.80", "19.20", "18.08", "3.92", "40.00"},
            {"33.00", "828000", "53.50", "21.50", "20.55", "4.45", "0"}
    }, new String[]{"31.33", "32.00", "30.13", "6.54"}),

    V(List.of(IRPJ, CSLL, COFINS, PIS_PASEP, CPP, ISS), new String[][]{
            {"15.50", "0", "25.00", "15.00", "14.10", "3.05", "28.85", "14.00"},
            {"18.00", "4500", "23.00", "15.00", "14.10", "3.05", "27.85", "17.00"},
            {"19.50", "9900", "24.00", "15.00", "14.92", "3.23", "23.85", "19.00"},
            {"20.50", "17100", "21.00", "15.00", "15.74", "3.41", "23.85", "21.00"},
            {"23.00", "62100", "23.00", "12.50", "14.10", "3.05", "23.85", "23.50"},
            {"30.50", "540000", "35.00", "15.50", "16.44", "3.56", "29.50", "0"}
    }, null);

    public record Bracket(int number, BigDecimal upperLimit, BigDecimal nominalRate, BigDecimal deduction,
                          Map<TaxType, BigDecimal> shares) {}

    private final List<Bracket> brackets;
    private final Map<TaxType, BigDecimal> issCapRedistribution;

    SimplesAnnex(List<TaxType> taxes, String[][] rows, String[] issCapRedistribution) {
        List<Bracket> list = new ArrayList<>();
        for (int i = 0; i < rows.length; i++) {
            Map<TaxType, BigDecimal> shares = new EnumMap<>(TaxType.class);
            for (int t = 0; t < taxes.size(); t++) {
                BigDecimal share = percent(rows[i][t + 2]);
                // 6ª faixa: ICMS/ISS are paid outside the DAS, so the table has no share for them
                if (share.signum() > 0) shares.put(taxes.get(t), share);
            }
            list.add(new Bracket(i + 1, TaxRules.SIMPLES_BRACKET_LIMITS.get(i), percent(rows[i][0]),
                    new BigDecimal(rows[i][1]), Collections.unmodifiableMap(shares)));
        }
        this.brackets = List.copyOf(list);
        if (issCapRedistribution == null) {
            this.issCapRedistribution = null;
        } else {
            Map<TaxType, BigDecimal> redistribution = new EnumMap<>(TaxType.class);
            for (int t = 0; t < issCapRedistribution.length; t++) {
                redistribution.put(taxes.get(t), percent(issCapRedistribution[t]));
            }
            this.issCapRedistribution = Collections.unmodifiableMap(redistribution);
        }
    }

    private static BigDecimal percent(String value) {
        return new BigDecimal(value).movePointLeft(2);
    }

    /** Returns the faixa for a RBT12 already known to be within the Simples limit. */
    public Bracket bracketFor(BigDecimal rbt12) {
        return brackets.stream()
                .filter(b -> rbt12.compareTo(b.upperLimit()) <= 0)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("RBT12 acima do limite do Simples Nacional."));
    }

    public List<Bracket> brackets() {
        return brackets;
    }

    /** Federal split of (alíquota efetiva − 5%) when the ISS is capped; null for annexes without the cap. */
    public Map<TaxType, BigDecimal> issCapRedistribution() {
        return issCapRedistribution;
    }
}
