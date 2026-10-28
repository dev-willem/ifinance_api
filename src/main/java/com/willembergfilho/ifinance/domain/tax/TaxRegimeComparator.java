package com.willembergfilho.ifinance.domain.tax;

import com.willembergfilho.ifinance.domain.math.MonetaryRounding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;
import static com.willembergfilho.ifinance.domain.tax.TaxType.*;

/** Annual tax burden of the same business under Simples Nacional, Lucro Presumido and Lucro Real. */
public class TaxRegimeComparator {

    private final SimplesNacionalCalculator simplesCalculator = new SimplesNacionalCalculator();

    public TaxComparisonResult compare(TaxComparisonParameters p) {
        validate(p);
        List<RegimeResult> regimes = List.of(simples(p), presumido(p), real(p));
        TaxRegime cheapest = regimes.stream()
                .filter(RegimeResult::eligible)
                .min(Comparator.comparing(RegimeResult::total))
                .map(RegimeResult::regime)
                .orElse(null);
        return new TaxComparisonResult(regimes, cheapest, TaxRules.DISCLAIMER);
    }

    private void validate(TaxComparisonParameters p) {
        if (p.activity() == null) throw new IllegalArgumentException("Informe a atividade da empresa.");
        if (p.annualRevenue() == null || p.annualRevenue().signum() <= 0)
            throw new IllegalArgumentException("A receita anual deve ser maior que zero.");
        if (p.annualPayroll().signum() < 0 || p.deductibleExpenses().signum() < 0 || p.creditablePurchases().signum() < 0)
            throw new IllegalArgumentException("Folha, custos e compras não podem ser negativos.");
        if (!isFraction(p.payrollChargesRate()) || !isFraction(p.salesTaxRate()))
            throw new IllegalArgumentException("As alíquotas devem estar entre 0% e 100%.");
        if (p.creditablePurchases().compareTo(p.deductibleExpenses()) > 0)
            throw new IllegalArgumentException("As compras com crédito de PIS/Cofins devem estar incluídas nos custos e despesas dedutíveis.");
    }

    private static boolean isFraction(BigDecimal rate) {
        return rate.signum() >= 0 && rate.compareTo(BigDecimal.ONE) <= 0;
    }

    private RegimeResult simples(TaxComparisonParameters p) {
        BusinessActivity activity = p.activity();
        // LC 123/2006 art. 18, § 24: Fator R payroll = salaries + pró-labore + FGTS (CPP is inside the DAS in III/V)
        // shortcut: FGTS is applied to pró-labore too (which has none), slightly overstating the Fator R
        BigDecimal fatorRPayroll = activity.fatorR()
                ? p.annualPayroll().multiply(BigDecimal.ONE.add(TaxRules.FGTS_RATE), MC) : null;
        // Steady state: RBT12 equals the annual revenue, so the yearly DAS is revenue × alíquota efetiva
        SimplesResult s = simplesCalculator.calculate(new SimplesParameters(p.annualRevenue(), p.annualRevenue(),
                activity.simplesAnnex(), fatorRPayroll));
        List<String> notes = new ArrayList<>(s.notes());
        if (!s.eligible()) return ineligible(TaxRegime.SIMPLES_NACIONAL, notes);

        notes.addFirst("Tributado pelo Anexo " + s.appliedAnnex() + ", " + s.bracket() + "ª faixa.");
        Map<TaxType, BigDecimal> taxes = new EnumMap<>(TaxType.class);
        s.shares().forEach(share -> taxes.put(share.tax(), share.amount()));
        if (s.appliedAnnex() == SimplesAnnex.IV) {
            taxes.merge(CPP, round(p.annualPayroll().multiply(TaxRules.ANNEX_IV_CPP_RATE, MC)), BigDecimal::add);
            notes.add("Anexo IV: CPP paga fora do DAS, estimada em 22% da folha (INSS patronal 20% + RAT 2%, "
                    + "sem terceiros); os encargos informados valem só para o Presumido e o Real.");
        }
        if (s.aboveSublimit()) {
            taxes.merge(activity.salesTax(), salesTax(p), BigDecimal::add);
        }
        return result(TaxRegime.SIMPLES_NACIONAL, p, taxes, notes);
    }

    private RegimeResult presumido(TaxComparisonParameters p) {
        BigDecimal revenue = p.annualRevenue();
        if (revenue.compareTo(TaxRules.PRESUMIDO_LIMIT) > 0) {
            return ineligible(TaxRegime.LUCRO_PRESUMIDO, List.of(
                    "Receita anual acima de R$ 78 milhões: Lucro Real obrigatório (Lei 9.718/1998, art. 13)."));
        }
        List<String> notes = new ArrayList<>();
        if (revenue.compareTo(TaxRules.PRESUMPTION_SURCHARGE_THRESHOLD) > 0) {
            notes.add("Presunção acrescida de 10% sobre a receita acima de R$ 5 milhões (LC 224/2025).");
        }
        BigDecimal salesTax = salesTax(p);
        BigDecimal pisCofinsBase = pisCofinsBase(p, salesTax);

        Map<TaxType, BigDecimal> taxes = new EnumMap<>(TaxType.class);
        taxes.put(IRPJ, irpj(presumedBase(revenue, p.activity().irpjPresumption())));
        taxes.put(CSLL, round(presumedBase(revenue, p.activity().csllPresumption()).multiply(TaxRules.CSLL_RATE, MC)));
        taxes.put(COFINS, round(pisCofinsBase.multiply(TaxRules.COFINS_CUMULATIVE, MC)));
        taxes.put(PIS_PASEP, round(pisCofinsBase.multiply(TaxRules.PIS_CUMULATIVE, MC)));
        taxes.put(CPP, payrollCharges(p));
        taxes.put(p.activity().salesTax(), salesTax);
        return result(TaxRegime.LUCRO_PRESUMIDO, p, taxes, notes);
    }

    private RegimeResult real(TaxComparisonParameters p) {
        List<String> notes = new ArrayList<>();
        BigDecimal salesTax = salesTax(p);
        BigDecimal pisCofinsBase = pisCofinsBase(p, salesTax);
        BigDecimal pisDebit = pisCofinsBase.multiply(TaxRules.PIS_NON_CUMULATIVE, MC);
        BigDecimal cofinsDebit = pisCofinsBase.multiply(TaxRules.COFINS_NON_CUMULATIVE, MC);
        BigDecimal pisCredit = p.creditablePurchases().multiply(TaxRules.PIS_NON_CUMULATIVE, MC);
        BigDecimal cofinsCredit = p.creditablePurchases().multiply(TaxRules.COFINS_NON_CUMULATIVE, MC);
        if (pisCredit.add(cofinsCredit).compareTo(pisDebit.add(cofinsDebit)) > 0) {
            notes.add("Créditos de PIS/Cofins superam os débitos: o saldo credor fica para os períodos seguintes.");
        }
        BigDecimal payrollCharges = payrollCharges(p);

        // Purchases are booked net of recoverable PIS/Cofins, so credits reduce cost
        BigDecimal taxableProfit = p.annualRevenue()
                .subtract(salesTax)
                .subtract(pisDebit.add(cofinsDebit), MC)
                .add(pisCredit.add(cofinsCredit), MC)
                .subtract(p.deductibleExpenses())
                .subtract(p.annualPayroll())
                .subtract(payrollCharges);

        Map<TaxType, BigDecimal> taxes = new EnumMap<>(TaxType.class);
        if (taxableProfit.signum() > 0) {
            taxes.put(IRPJ, irpj(taxableProfit));
            taxes.put(CSLL, round(taxableProfit.multiply(TaxRules.CSLL_RATE, MC)));
        } else {
            taxes.put(IRPJ, round(BigDecimal.ZERO));
            taxes.put(CSLL, round(BigDecimal.ZERO));
            notes.add("Prejuízo fiscal: sem IRPJ e CSLL; o prejuízo pode compensar até 30% do lucro de anos seguintes.");
        }
        taxes.put(COFINS, round(cofinsDebit.subtract(cofinsCredit).max(BigDecimal.ZERO)));
        taxes.put(PIS_PASEP, round(pisDebit.subtract(pisCredit).max(BigDecimal.ZERO)));
        taxes.put(CPP, payrollCharges);
        taxes.put(p.activity().salesTax(), salesTax);
        return result(TaxRegime.LUCRO_REAL, p, taxes, notes);
    }

    /** Lei 9.249/1995 arts. 15 e 20, plus the LC 224/2025 surcharge on revenue above R$ 5 mi. */
    private static BigDecimal presumedBase(BigDecimal revenue, BigDecimal presumption) {
        BigDecimal excess = revenue.subtract(TaxRules.PRESUMPTION_SURCHARGE_THRESHOLD).max(BigDecimal.ZERO);
        return revenue.multiply(presumption, MC)
                .add(excess.multiply(presumption, MC).multiply(TaxRules.PRESUMPTION_SURCHARGE, MC), MC);
    }

    private static BigDecimal irpj(BigDecimal base) {
        BigDecimal surtaxBase = base.subtract(TaxRules.IRPJ_SURTAX_ANNUAL_THRESHOLD).max(BigDecimal.ZERO);
        return round(base.multiply(TaxRules.IRPJ_RATE, MC).add(surtaxBase.multiply(TaxRules.IRPJ_SURTAX_RATE, MC), MC));
    }

    private static BigDecimal salesTax(TaxComparisonParameters p) {
        return round(p.annualRevenue().multiply(p.salesTaxRate(), MC));
    }

    // STF Tema 69 (RE 574.706): ICMS is excluded from the PIS/Cofins base; ISS is not
    private static BigDecimal pisCofinsBase(TaxComparisonParameters p, BigDecimal salesTax) {
        return p.activity().salesTax() == ICMS ? p.annualRevenue().subtract(salesTax) : p.annualRevenue();
    }

    private static BigDecimal payrollCharges(TaxComparisonParameters p) {
        return round(p.annualPayroll().multiply(p.payrollChargesRate(), MC));
    }

    private static RegimeResult ineligible(TaxRegime regime, List<String> notes) {
        return new RegimeResult(regime, false, List.of(), null, null, null, List.copyOf(notes));
    }

    private static RegimeResult result(TaxRegime regime, TaxComparisonParameters p,
                                       Map<TaxType, BigDecimal> taxes, List<String> notes) {
        BigDecimal total = taxes.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        List<TaxShare> breakdown = taxes.entrySet().stream()
                .map(e -> new TaxShare(e.getKey(),
                        total.signum() == 0 ? BigDecimal.ZERO : MonetaryRounding.roundRate(e.getValue().divide(total, MC)),
                        e.getValue()))
                .toList();
        BigDecimal netProfit = p.annualRevenue()
                .subtract(p.deductibleExpenses())
                .subtract(p.annualPayroll())
                .subtract(total);
        return new RegimeResult(regime, true, breakdown, total,
                MonetaryRounding.roundRate(total.divide(p.annualRevenue(), MC)),
                round(netProfit), List.copyOf(notes));
    }
}
