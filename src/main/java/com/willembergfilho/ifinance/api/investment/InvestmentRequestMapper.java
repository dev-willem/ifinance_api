package com.willembergfilho.ifinance.api.investment;

import com.willembergfilho.ifinance.domain.investment.InvestmentParameters;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface InvestmentRequestMapper {

    @Mapping(source = "investmentType", target = "type")
    InvestmentParameters toParameters(InvestmentRequest request);
}
