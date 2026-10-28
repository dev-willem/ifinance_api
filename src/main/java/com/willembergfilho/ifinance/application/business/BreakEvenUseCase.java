package com.willembergfilho.ifinance.application.business;

import com.willembergfilho.ifinance.domain.business.BreakEvenParameters;
import com.willembergfilho.ifinance.domain.business.BreakEvenResult;
import com.willembergfilho.ifinance.domain.business.Ratios;
import com.willembergfilho.ifinance.domain.math.CalculationException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MC;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.MODE;
import static com.willembergfilho.ifinance.domain.math.MonetaryRounding.round;

@Service
public class BreakEvenUseCase {

    private static final int UNITS_SCALE = 4;

    private record Point(BigDecimal exact, BigDecimal units, BigDecimal revenue) {}

    public BreakEvenResult execute(BreakEvenParameters p) {
        BigDecimal mcu = p.unitPrice().subtract(p.unitVariableCost());
        if (mcu.signum() <= 0) {
            throw new CalculationException("O preço de venda unitário deve ser maior que o custo variável unitário: "
                    + "sem margem de contribuição positiva não existe ponto de equilíbrio.");
        }

        BigDecimal accountingQuantity = p.fixedCosts().divide(mcu, MC);
        Point accounting = point(accountingQuantity, p.unitPrice());
        Point economic = p.desiredProfit() == null ? null
                : point(p.fixedCosts().add(p.desiredProfit()).divide(mcu, MC), p.unitPrice());
        Point financial = point(p.fixedCosts().subtract(p.depreciation()).divide(mcu, MC), p.unitPrice());

        BigDecimal currentRevenue = null, operatingProfit = null, safetyUnits = null,
                safetyRevenue = null, safetyRatio = null, leverage = null;
        BigDecimal q = p.currentQuantity();
        if (q != null) {
            BigDecimal contribution = q.multiply(mcu, MC);
            BigDecimal profit = contribution.subtract(p.fixedCosts());
            BigDecimal safetyQuantity = q.subtract(accountingQuantity, MC);
            currentRevenue = round(q.multiply(p.unitPrice(), MC));
            operatingProfit = round(profit);
            safetyUnits = safetyQuantity.setScale(UNITS_SCALE, MODE);
            safetyRevenue = round(safetyQuantity.multiply(p.unitPrice(), MC));
            safetyRatio = Ratios.of(safetyQuantity, q);
            // GAO = MC total / lucro operacional; undefined (null) exactly at break-even.
            leverage = Ratios.of(contribution, profit);
        }

        return new BreakEvenResult(
                round(mcu),
                Ratios.of(mcu, p.unitPrice()),
                accounting.exact(), accounting.units(), accounting.revenue(),
                economic != null ? economic.exact() : null,
                economic != null ? economic.units() : null,
                economic != null ? economic.revenue() : null,
                financial.exact(), financial.units(), financial.revenue(),
                currentRevenue, operatingProfit, safetyUnits, safetyRevenue, safetyRatio, leverage
        );
    }

    private static Point point(BigDecimal quantity, BigDecimal unitPrice) {
        return new Point(
                quantity.setScale(UNITS_SCALE, MODE),
                quantity.setScale(0, RoundingMode.CEILING),
                round(quantity.multiply(unitPrice, MC)));
    }
}
