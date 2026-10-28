package com.willembergfilho.ifinance.api.risk;

import com.willembergfilho.ifinance.application.risk.RiskMetricsUseCase;
import com.willembergfilho.ifinance.domain.risk.RiskMetricsParameters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/risk")
@Tag(name = "risk")
@SecurityRequirement(name = "oauth2")
public class RiskMetricsController {

    private final RiskMetricsUseCase useCase;

    public RiskMetricsController(RiskMetricsUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/metrics")
    @Operation(summary = "Calculate risk metrics (Sharpe, Sortino, drawdown, VaR, beta, alpha) for a return or price series")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Risk metrics calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Series too short or values too extreme")
    })
    public ResponseEntity<RiskMetricsResponse> metrics(@Valid @RequestBody RiskMetricsRequest request) {
        RiskMetricsParameters params = new RiskMetricsParameters(
                request.values(),
                request.inputType(),
                request.benchmarkValues(),
                request.periodicity(),
                request.riskFreeAnnualRate() != null ? request.riskFreeAnnualRate().doubleValue() : 0,
                request.confidenceLevel() != null ? request.confidenceLevel().doubleValue() : 0.95
        );
        return ResponseEntity.ok(RiskMetricsResponse.from(useCase.execute(params)));
    }
}
