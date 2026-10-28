package com.willembergfilho.ifinance.domain.tax;

import com.willembergfilho.ifinance.domain.math.MonetaryRounding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;

public class SimplesNacionalCalculator {

    public SimplesResult calculate(SimplesParameters p) {
        if (p.annex() == null) throw new IllegalArgumentException("Informe o anexo do Simples Nacional.");
        if (p.rbt12() == null || p.rbt12().signum() <= 0)
            throw new IllegalArgumentException("A receita bruta dos últimos 12 meses (RBT12) deve ser maior que zero.");
        if (p.monthlyRevenue() == null || p.monthlyRevenue().signum() < 0)
            throw new IllegalArgumentException("A receita do mês não pode ser negativa.");
        if (p.payroll12() != null && p.payroll12().signum() < 0)
            throw new IllegalArgumentException("A folha de salários não pode ser negativa.");

        List<String> notes = new ArrayList<>();
        SimplesAnnex annex = p.annex();
        BigDecimal fatorR = null;
        if (p.payroll12() != null && (annex == SimplesAnnex.III || annex == SimplesAnnex.V)) {
            BigDecimal ratio = p.payroll12().divide(p.rbt12(), MC);
            fatorR = MonetaryRounding.roundRate(ratio);
            annex = ratio.compareTo(TaxRules.FATOR_R_THRESHOLD) >= 0 ? SimplesAnnex.III : SimplesAnnex.V;
            notes.add(annex == SimplesAnnex.III
                    ? "Fator R maior ou igual a 28%: atividade tributada pelo Anexo III."
                    : "Fator R abaixo de 28%: atividade tributada pelo Anexo V.");
        }

        if (p.rbt12().compareTo(TaxRules.SIMPLES_LIMIT) > 0) {
            notes.add("RBT12 acima de R$ 4,8 milhões: a empresa não pode permanecer no Simples Nacional (LC 123/2006, art. 3º, II).");
            return new SimplesResult(p.annex(), annex, fatorR, false, true,
                    null, null, null, null, null, List.of(), notes);
        }

        boolean aboveSublimit = p.rbt12().compareTo(TaxRules.SIMPLES_SUBLIMIT) > 0;
        if (aboveSublimit) {
            notes.add("RBT12 acima do sublimite de R$ 3,6 milhões: ICMS e ISS são recolhidos fora do DAS, pelo regime normal.");
        }
        if (annex == SimplesAnnex.IV) {
            notes.add("Anexo IV: a CPP (INSS patronal) não está no DAS e é recolhida à parte sobre a folha.");
        }

        SimplesAnnex.Bracket bracket = annex.bracketFor(p.rbt12());
        // LC 123/2006 art. 18, § 1º-A: (RBT12 × Aliq − PD) / RBT12
        BigDecimal effectiveRate = p.rbt12().multiply(bracket.nominalRate(), MC)
                .subtract(bracket.deduction(), MC)
                .divide(p.rbt12(), MC);
        BigDecimal das = MonetaryRounding.round(p.monthlyRevenue().multiply(effectiveRate, MC));

        return new SimplesResult(p.annex(), annex, fatorR, true, aboveSublimit,
                bracket.number(), bracket.nominalRate(), bracket.deduction(),
                MonetaryRounding.roundRate(effectiveRate), das,
                split(das, p.monthlyRevenue(), effectiveRate, taxRates(annex, bracket, effectiveRate)),
                notes);
    }

    /** Effective rate of each tax on revenue, applying the 5% ISS cap of the 5ª faixa (Anexos III e IV). */
    private Map<TaxType, BigDecimal> taxRates(SimplesAnnex annex, SimplesAnnex.Bracket bracket, BigDecimal effectiveRate) {
        Map<TaxType, BigDecimal> rates = new EnumMap<>(TaxType.class);
        BigDecimal issShare = bracket.shares().get(TaxType.ISS);
        boolean issCapped = issShare != null && annex.issCapRedistribution() != null
                && effectiveRate.multiply(issShare, MC).compareTo(TaxRules.SIMPLES_ISS_CAP) > 0;
        if (issCapped) {
            BigDecimal federal = effectiveRate.subtract(TaxRules.SIMPLES_ISS_CAP, MC);
            annex.issCapRedistribution().forEach((tax, pct) -> rates.put(tax, federal.multiply(pct, MC)));
            rates.put(TaxType.ISS, TaxRules.SIMPLES_ISS_CAP);
        } else {
            bracket.shares().forEach((tax, pct) -> rates.put(tax, effectiveRate.multiply(pct, MC)));
        }
        return rates;
    }

    private List<TaxShare> split(BigDecimal das, BigDecimal revenue, BigDecimal effectiveRate,
                                 Map<TaxType, BigDecimal> rates) {
        Map<TaxType, BigDecimal> amounts = new EnumMap<>(TaxType.class);
        rates.forEach((tax, rate) -> amounts.put(tax, MonetaryRounding.round(revenue.multiply(rate, MC))));
        // Rounding cents go to the tax with the largest share, as LC 123/2006 does with the percentages
        BigDecimal residual = das.subtract(amounts.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        TaxType largest = rates.entrySet().stream().max(Map.Entry.comparingByValue()).orElseThrow().getKey();
        amounts.merge(largest, residual, BigDecimal::add);

        return rates.entrySet().stream()
                .map(e -> new TaxShare(e.getKey(),
                        MonetaryRounding.roundRate(e.getValue().divide(effectiveRate, MC)),
                        amounts.get(e.getKey())))
                .toList();
    }
}
