package com.willembergfilho.ifinance.api.tax;

import com.willembergfilho.ifinance.application.tax.SimplesNacionalUseCase;
import com.willembergfilho.ifinance.application.tax.TaxRegimeCompareUseCase;
import com.willembergfilho.ifinance.domain.tax.SimplesParameters;
import com.willembergfilho.ifinance.domain.tax.TaxComparisonParameters;
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
@RequestMapping("/api/v1/tax")
@Tag(name = "tax")
@SecurityRequirement(name = "oauth2")
public class TaxController {

    private final SimplesNacionalUseCase simplesUseCase;
    private final TaxRegimeCompareUseCase compareUseCase;

    public TaxController(SimplesNacionalUseCase simplesUseCase,
                         TaxRegimeCompareUseCase compareUseCase) {
        this.simplesUseCase = simplesUseCase;
        this.compareUseCase = compareUseCase;
    }

    @PostMapping("/simples")
    @Operation(summary = "Calculate the monthly Simples Nacional DAS (faixa, alíquota efetiva, Fator R, repartição)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "DAS calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<SimplesResponse> simples(@Valid @RequestBody SimplesRequest request) {
        SimplesParameters params = new SimplesParameters(
                request.rbt12(),
                request.monthlyRevenue(),
                request.annex(),
                request.payroll12()
        );
        return ResponseEntity.ok(SimplesResponse.from(simplesUseCase.execute(params)));
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare the annual tax burden under Simples Nacional, Lucro Presumido and Lucro Real")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Regimes compared"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<TaxCompareResponse> compare(@Valid @RequestBody TaxCompareRequest request) {
        TaxComparisonParameters params = new TaxComparisonParameters(
                request.annualRevenue(),
                request.activity(),
                request.annualPayroll(),
                request.payrollChargesRate(),
                request.deductibleExpenses(),
                request.creditablePurchases(),
                request.salesTaxRate()
        );
        return ResponseEntity.ok(TaxCompareResponse.from(compareUseCase.execute(params)));
    }
}
