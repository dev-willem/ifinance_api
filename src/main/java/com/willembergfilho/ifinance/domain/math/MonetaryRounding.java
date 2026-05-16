package com.willembergfilho.ifinance.domain.math;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public final class MonetaryRounding {

    public static final MathContext MC = MathContext.DECIMAL128;
    public static final RoundingMode MODE = RoundingMode.HALF_EVEN;
    public static final int MONETARY_SCALE = 2;
    public static final int RATE_SCALE = 10;

    private MonetaryRounding() {}

    public static BigDecimal round(BigDecimal value) {
        return value.setScale(MONETARY_SCALE, MODE);
    }

    public static BigDecimal roundRate(BigDecimal rate) {
        return rate.setScale(RATE_SCALE, MODE);
    }
}
