package com.willembergfilho.ifinance.domain.tax;

import java.math.BigDecimal;

/** {@code payroll12} (folha dos últimos 12 meses, com encargos) is optional and only drives the Fator R. */
public record SimplesParameters(
        BigDecimal rbt12,
        BigDecimal monthlyRevenue,
        SimplesAnnex annex,
        BigDecimal payroll12
) {}
