package com.willembergfilho.ifinance.domain.business;

import java.math.BigDecimal;
import java.util.List;

import static com.willembergfilho.ifinance.domain.business.Ratios.*;

/** All rates are fractions of the SELLING PRICE (markup divisor method). currentPrice is optional. */
public record MarkupParameters(
        BigDecimal unitCost,
        BigDecimal salesTaxRate,
        BigDecimal commissionRate,
        BigDecimal cardFeeRate,
        BigDecimal fixedExpenseRate,
        BigDecimal desiredProfitRate,
        List<NamedRate> extraRates,
        BigDecimal currentPrice
) {
    public record NamedRate(String name, BigDecimal rate) {
        public NamedRate {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Informe o nome de cada percentual adicional.");
            }
            requireRate(rate, "O percentual '" + name + "'");
        }
    }

    public MarkupParameters {
        salesTaxRate = orZero(salesTaxRate);
        commissionRate = orZero(commissionRate);
        cardFeeRate = orZero(cardFeeRate);
        fixedExpenseRate = orZero(fixedExpenseRate);
        desiredProfitRate = orZero(desiredProfitRate);
        extraRates = extraRates != null ? List.copyOf(extraRates) : List.of();

        if (unitCost == null || unitCost.signum() <= 0) {
            throw new IllegalArgumentException("O custo unitário deve ser maior que zero.");
        }
        requireRate(salesTaxRate, "O percentual de impostos sobre vendas");
        requireRate(commissionRate, "O percentual de comissão");
        requireRate(cardFeeRate, "A taxa de cartão");
        requireRate(fixedExpenseRate, "O percentual de despesas fixas");
        requireRate(desiredProfitRate, "A margem de lucro desejada");
        if (currentPrice != null && currentPrice.signum() <= 0) {
            throw new IllegalArgumentException("O preço atual deve ser maior que zero.");
        }
    }
}
