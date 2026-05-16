package com.willembergfilho.ifinance.infrastructure.bcb;

import com.fasterxml.jackson.annotation.JsonProperty;

public record BcbIndexResponse(
        @JsonProperty("data") String date,
        @JsonProperty("valor") String value
) {}
