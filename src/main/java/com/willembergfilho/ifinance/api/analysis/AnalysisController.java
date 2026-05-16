package com.willembergfilho.ifinance.api.analysis;

import com.willembergfilho.ifinance.application.analysis.FinancialAnalysisUseCase;
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
@RequestMapping("/api/v1/analysis")
@Tag(name = "analysis")
@SecurityRequirement(name = "oauth2")
public class AnalysisController {

    private final FinancialAnalysisUseCase financialAnalysis;

    public AnalysisController(FinancialAnalysisUseCase financialAnalysis) {
        this.financialAnalysis = financialAnalysis;
    }

    @PostMapping
    @Operation(summary = "Calculate NPV, IRR, and payback for a cash flow series")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analysis result"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Calculation error")
    })
    public ResponseEntity<AnalysisResponse> analyze(@Valid @RequestBody AnalysisRequest request) {
        FinancialAnalysisUseCase.AnalysisResult result =
                financialAnalysis.execute(request.cashFlows(), request.discountRate());
        return ResponseEntity.ok(AnalysisResponse.from(result));
    }
}
