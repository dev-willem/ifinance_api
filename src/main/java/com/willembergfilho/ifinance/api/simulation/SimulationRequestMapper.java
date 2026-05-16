package com.willembergfilho.ifinance.api.simulation;

import com.willembergfilho.ifinance.domain.simulation.CetCharge;
import com.willembergfilho.ifinance.domain.simulation.SimulationParameters;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
public interface SimulationRequestMapper {

    SimulationParameters toParameters(SimulationRequest request);

    CetCharge toCetCharge(CetChargeRequest request);
}
