package com.willembergfilho.ifinance.domain.credit;

import com.willembergfilho.ifinance.domain.math.MonetaryRounding;

import java.math.BigDecimal;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;

/**
 * Each modality is reduced to the ratio r = N / A (face value over present value):
 * rational simple 1 + i n; commercial simple 1 / (1 - i n); rational compound (1 + i)^n; commercial compound (1 - i)^-n.
 */
public class DiscountCalculator {

    /** Beyond this ratio (PV below one millionth of N) or per-period rate (100,000,000%) the result is meaningless. */
    private static final double MAX_RATIO = 1e6;
    private static final String OUT_OF_RANGE = "Resultado fora da faixa numérica para a taxa e o prazo informados.";

    public List<DiscountResult> calculate(DiscountParameters p) {
        validate(p);
        BigDecimal i = p.ratePerPeriod();
        BigDecimal n = p.periods();
        BigDecimal in = i.multiply(n, MC);
        double iD = i.doubleValue();
        double nD = n.doubleValue();

        return List.of(
                result(DiscountModality.RATIONAL_SIMPLE, p, BigDecimal.ONE.add(in, MC)),
                in.compareTo(BigDecimal.ONE) >= 0
                        ? DiscountResult.invalid(DiscountModality.COMMERCIAL_SIMPLE,
                                "Desconto comercial simples inválido: taxa x prazo (i·n) deve ser menor que 100%, senão o valor presente seria zero ou negativo.")
                        : result(DiscountModality.COMMERCIAL_SIMPLE, p,
                                BigDecimal.ONE.divide(BigDecimal.ONE.subtract(in, MC), MC)),
                fromDouble(DiscountModality.RATIONAL_COMPOUND, p, Math.pow(1 + iD, nD)),
                iD >= 1
                        ? DiscountResult.invalid(DiscountModality.COMMERCIAL_COMPOUND,
                                "Desconto comercial composto inválido: a taxa por período deve ser menor que 100%.")
                        : fromDouble(DiscountModality.COMMERCIAL_COMPOUND, p, Math.pow(1 - iD, -nD))
        );
    }

    private static DiscountResult fromDouble(DiscountModality modality, DiscountParameters p, double ratio) {
        if (!Double.isFinite(ratio)) {
            return DiscountResult.invalid(modality, OUT_OF_RANGE);
        }
        return result(modality, p, new BigDecimal(String.valueOf(ratio)));
    }

    private static DiscountResult result(DiscountModality modality, DiscountParameters p, BigDecimal ratio) {
        BigDecimal presentValue = MonetaryRounding.round(p.faceValue().divide(ratio, MC));
        double compoundPerPeriod = Math.pow(ratio.doubleValue(), 1.0 / p.periods().doubleValue()) - 1;
        // negated <= also rejects NaN and Infinity
        if (presentValue.signum() == 0 || !(ratio.doubleValue() <= MAX_RATIO) || !(compoundPerPeriod <= MAX_RATIO)) {
            return DiscountResult.invalid(modality, OUT_OF_RANGE);
        }
        BigDecimal total = ratio.subtract(BigDecimal.ONE, MC);
        return new DiscountResult(
                modality,
                true,
                presentValue,
                p.faceValue().subtract(presentValue),
                MonetaryRounding.roundRate(total),
                MonetaryRounding.roundRate(total.divide(p.periods(), MC)),
                MonetaryRounding.roundRate(new BigDecimal(String.valueOf(compoundPerPeriod))),
                null
        );
    }

    private static void validate(DiscountParameters p) {
        if (p.faceValue() == null || p.faceValue().signum() <= 0) {
            throw new IllegalArgumentException("O valor nominal deve ser maior que zero.");
        }
        if (p.ratePerPeriod() == null || p.ratePerPeriod().signum() <= 0) {
            throw new IllegalArgumentException("A taxa por período deve ser maior que zero.");
        }
        if (p.periods() == null || p.periods().signum() <= 0) {
            throw new IllegalArgumentException("O número de períodos deve ser maior que zero.");
        }
    }
}
