package com.willembergfilho.ifinance.application.business;

import com.willembergfilho.ifinance.domain.business.MarkupParameters;
import com.willembergfilho.ifinance.domain.business.MarkupParameters.NamedRate;
import com.willembergfilho.ifinance.domain.business.MarkupResult;
import com.willembergfilho.ifinance.domain.business.MarkupResult.PriceComponent;
import com.willembergfilho.ifinance.domain.business.Ratios;
import com.willembergfilho.ifinance.domain.math.CalculationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MODE;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;

@Service
public class MarkupUseCase {

    /** Below 1% left for the cost the multiplier exceeds 100x, which is never a meaningful price. */
    private static final BigDecimal MIN_DIVISOR = new BigDecimal("0.01");

    private record Share(String key, String label, BigDecimal rate) {}

    public MarkupResult execute(MarkupParameters p) {
        List<Share> expenses = new ArrayList<>();
        expenses.add(new Share("SALES_TAX", "Impostos sobre vendas", p.salesTaxRate()));
        expenses.add(new Share("COMMISSION", "Comissão", p.commissionRate()));
        expenses.add(new Share("CARD_FEE", "Taxa de cartão", p.cardFeeRate()));
        expenses.add(new Share("FIXED_EXPENSE", "Despesas fixas", p.fixedExpenseRate()));
        for (NamedRate extra : p.extraRates()) {
            expenses.add(new Share("EXTRA", extra.name().strip(), extra.rate()));
        }
        expenses.removeIf(s -> s.rate().signum() == 0);

        BigDecimal expenseRate = expenses.stream().map(Share::rate).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalRate = expenseRate.add(p.desiredProfitRate());
        if (totalRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new CalculationException("A soma dos percentuais sobre o preço de venda ("
                    + percent(totalRate) + ") deve ser inferior a 100%. Reduza impostos, despesas ou a margem de lucro.");
        }

        BigDecimal divisor = BigDecimal.ONE.subtract(totalRate);
        if (divisor.compareTo(MIN_DIVISOR) < 0) {
            throw new CalculationException("A soma dos percentuais (" + percent(totalRate)
                    + ") deixa margem inferior a 1% para o custo, o que multiplicaria o preço por mais de 100 vezes.");
        }
        BigDecimal price = round(p.unitCost().divide(divisor, MC));

        List<PriceComponent> composition = new ArrayList<>();
        BigDecimal cost = round(p.unitCost());
        BigDecimal allocated = cost;
        composition.add(new PriceComponent("COST", "Custo unitário", Ratios.of(cost, price), cost));
        for (Share s : expenses) {
            BigDecimal amount = round(price.multiply(s.rate(), MC));
            allocated = allocated.add(amount);
            composition.add(new PriceComponent(s.key(), s.label(), s.rate(), amount));
        }
        // The cent left over by rounding goes to profit (or, at 0% profit, to the largest expense)
        // so the composition adds up exactly to the price.
        BigDecimal remainder = price.subtract(allocated);
        if (p.desiredProfitRate().signum() > 0) {
            composition.add(new PriceComponent("PROFIT", "Lucro", p.desiredProfitRate(), remainder));
        } else if (remainder.signum() != 0) {
            // Without expenses price == cost, so a non-zero remainder implies at least one expense line.
            PriceComponent largest = composition.subList(1, composition.size()).stream()
                    .max(Comparator.comparing(PriceComponent::amount)).orElseThrow();
            composition.set(composition.indexOf(largest), new PriceComponent(
                    largest.key(), largest.label(), largest.rate(), largest.amount().add(remainder)));
        }

        BigDecimal actualProfit = null, actualMargin = null;
        if (p.currentPrice() != null) {
            BigDecimal profit = p.currentPrice().multiply(BigDecimal.ONE.subtract(expenseRate), MC)
                    .subtract(p.unitCost(), MC);
            actualProfit = round(profit);
            actualMargin = Ratios.of(profit, p.currentPrice());
        }

        return new MarkupResult(
                totalRate.setScale(Ratios.RATIO_SCALE, MODE),
                divisor.setScale(Ratios.RATIO_SCALE, MODE),
                Ratios.of(BigDecimal.ONE, divisor),
                price,
                List.copyOf(composition),
                p.currentPrice() != null ? round(p.currentPrice()) : null,
                actualProfit,
                actualMargin
        );
    }

    private static String percent(BigDecimal rate) {
        return rate.movePointRight(2).setScale(2, MODE).toPlainString().replace('.', ',') + "%";
    }
}
