package com.willembergfilho.ifinance.api.investment;

import com.willembergfilho.ifinance.domain.investment.InvestmentParameters;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.math.BigDecimal;

@Mapper
public interface InvestmentRequestMapper {

    /**
     * rateValue chega em percentual (ex: 110 = 110% do CDI, 12.5 = 12,5% a.a.)
     * e o domínio trabalha em decimal (1.10, 0.125).
     */
    @Mapping(source = "investmentType", target = "type")
    @Mapping(source = "rateValue", target = "rateValue", qualifiedByName = "percentToDecimal")
    InvestmentParameters toParameters(InvestmentRequest request);

    @Named("percentToDecimal")
    default BigDecimal percentToDecimal(BigDecimal percent) {
        return percent != null ? percent.movePointLeft(2) : null;
    }
}
