package com.willembergfilho.ifinance.api.credit;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DiscountRequest(
        @NotNull(message = "O valor nominal é obrigatório.")
        @Positive(message = "O valor nominal deve ser maior que zero.")
        @DecimalMax(value = "100000000000", message = "O valor nominal excede o limite permitido.")
        BigDecimal faceValue,

        @NotNull(message = "A taxa por período é obrigatória.")
        @Positive(message = "A taxa por período deve ser maior que zero.")
        @DecimalMax(value = "10", message = "A taxa por período deve ser de no máximo 1000%.")
        BigDecimal ratePerPeriod,

        @NotNull(message = "O número de períodos é obrigatório.")
        @Positive(message = "O número de períodos deve ser maior que zero.")
        @DecimalMax(value = "1200", message = "O número de períodos deve ser de no máximo 1200.")
        BigDecimal periods
) {}
