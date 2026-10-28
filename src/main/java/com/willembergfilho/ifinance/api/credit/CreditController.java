package com.willembergfilho.ifinance.api.credit;

import com.willembergfilho.ifinance.application.credit.CreditCapacityUseCase;
import com.willembergfilho.ifinance.application.credit.DiscountUseCase;
import com.willembergfilho.ifinance.domain.credit.CreditCapacityParameters;
import com.willembergfilho.ifinance.domain.credit.DiscountParameters;
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
@RequestMapping("/api/v1/credit")
@Tag(name = "credit")
@SecurityRequirement(name = "oauth2")
public class CreditController {

    private final CreditCapacityUseCase capacityUseCase;
    private final DiscountUseCase discountUseCase;

    public CreditController(CreditCapacityUseCase capacityUseCase, DiscountUseCase discountUseCase) {
        this.capacityUseCase = capacityUseCase;
        this.discountUseCase = discountUseCase;
    }

    @PostMapping("/capacity")
    @Operation(summary = "Analyze credit capacity (max installment, max loan, approval and minimum term)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Credit capacity calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error or unsupported amortization system")
    })
    public ResponseEntity<CreditCapacityResponse> capacity(@Valid @RequestBody CreditCapacityRequest request) {
        CreditCapacityParameters params = new CreditCapacityParameters(
                request.monthlyIncome(),
                request.existingMonthlyDebts(),
                request.maxCommitmentRate(),
                request.annualInterestRate(),
                request.termMonths(),
                request.amortizationSystem(),
                request.desiredLoanAmount()
        );
        return ResponseEntity.ok(CreditCapacityResponse.from(capacityUseCase.execute(params)));
    }

    @PostMapping("/discount")
    @Operation(summary = "Compare rational/commercial simple and compound discount of a face value")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Discount modalities calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<DiscountResponse> discount(@Valid @RequestBody DiscountRequest request) {
        DiscountParameters params = new DiscountParameters(
                request.faceValue(),
                request.ratePerPeriod(),
                request.periods()
        );
        return ResponseEntity.ok(DiscountResponse.from(request.faceValue(), request.ratePerPeriod(),
                request.periods(), discountUseCase.execute(params)));
    }
}
