package com.willembergfilho.ifinance.api.planning;

import com.willembergfilho.ifinance.application.planning.EmergencyReserveUseCase;
import com.willembergfilho.ifinance.application.planning.RetirementPlanningUseCase;
import com.willembergfilho.ifinance.domain.planning.EmergencyReserveParameters;
import com.willembergfilho.ifinance.domain.planning.RetirementParameters;
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
@RequestMapping("/api/v1/planning")
@Tag(name = "planning")
@SecurityRequirement(name = "oauth2")
public class PlanningController {

    private final RetirementPlanningUseCase retirementUseCase;
    private final EmergencyReserveUseCase emergencyUseCase;

    public PlanningController(RetirementPlanningUseCase retirementUseCase,
                              EmergencyReserveUseCase emergencyUseCase) {
        this.retirementUseCase = retirementUseCase;
        this.emergencyUseCase = emergencyUseCase;
    }

    @PostMapping("/retirement")
    @Operation(summary = "Calculate FIRE retirement planning (4% rule)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Retirement plan calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<RetirementResponse> retirement(@Valid @RequestBody RetirementRequest request) {
        RetirementParameters params = new RetirementParameters(
                request.monthlyExpenses(),
                request.currentSavings(),
                request.monthlySavings(),
                request.expectedAnnualReturn(),
                request.withdrawalRate()
        );
        return ResponseEntity.ok(RetirementResponse.from(retirementUseCase.execute(params)));
    }

    @PostMapping("/emergency")
    @Operation(summary = "Calculate emergency reserve target and time to completion")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Emergency reserve calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<EmergencyResponse> emergency(@Valid @RequestBody EmergencyRequest request) {
        EmergencyReserveParameters params = new EmergencyReserveParameters(
                request.monthlyExpenses(),
                request.monthsCoverage(),
                request.currentSavings(),
                request.monthlySavings(),
                request.expectedAnnualReturn()
        );
        return ResponseEntity.ok(EmergencyResponse.from(emergencyUseCase.execute(params)));
    }
}
