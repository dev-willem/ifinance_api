package com.willembergfilho.ifinance.api.montecarlo;

import com.willembergfilho.ifinance.application.montecarlo.MonteCarloSimulationUseCase;
import com.willembergfilho.ifinance.domain.montecarlo.MonteCarloParameters;
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
public class MonteCarloController {

    private final MonteCarloSimulationUseCase useCase;

    public MonteCarloController(MonteCarloSimulationUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/monte-carlo")
    @Operation(summary = "Run a Monte Carlo simulation of portfolio growth (monthly log-normal returns)",
            description = "If annualInflation is given, targetAmount is read in today's money and inflated to the horizon "
                    + "(nominalTarget); all other monetary outputs are nominal.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation completed"),
            @ApiResponse(responseCode = "400", description = "Validation error or iterations × months above the limit"),
            @ApiResponse(responseCode = "422", description = "Simulated values exceeded numeric limits")
    })
    public ResponseEntity<MonteCarloResponse> simulate(@Valid @RequestBody MonteCarloRequest request) {
        MonteCarloParameters params = new MonteCarloParameters(
                request.initialAmount().doubleValue(),
                request.monthlyContribution().doubleValue(),
                request.expectedAnnualReturn().doubleValue(),
                request.annualVolatility().doubleValue(),
                request.years(),
                request.iterations() != null ? request.iterations() : MonteCarloRequest.DEFAULT_ITERATIONS,
                request.targetAmount() != null ? request.targetAmount().doubleValue() : null,
                request.annualInflation() != null ? request.annualInflation().doubleValue() : null,
                request.contributionGrowthRate() != null ? request.contributionGrowthRate().doubleValue() : 0,
                request.seed()
        );
        return ResponseEntity.ok(MonteCarloResponse.from(useCase.execute(params)));
    }
}
