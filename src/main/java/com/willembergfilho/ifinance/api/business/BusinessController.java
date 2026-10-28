package com.willembergfilho.ifinance.api.business;

import com.willembergfilho.ifinance.application.business.BreakEvenUseCase;
import com.willembergfilho.ifinance.application.business.IncomeStatementUseCase;
import com.willembergfilho.ifinance.application.business.MarkupUseCase;
import com.willembergfilho.ifinance.domain.business.BreakEvenParameters;
import com.willembergfilho.ifinance.domain.business.IncomeStatementParameters;
import com.willembergfilho.ifinance.domain.business.MarkupParameters;
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
@RequestMapping("/api/v1/business")
@Tag(name = "business")
@SecurityRequirement(name = "oauth2")
public class BusinessController {

    private final IncomeStatementUseCase incomeStatementUseCase;
    private final BreakEvenUseCase breakEvenUseCase;
    private final MarkupUseCase markupUseCase;

    public BusinessController(IncomeStatementUseCase incomeStatementUseCase,
                              BreakEvenUseCase breakEvenUseCase,
                              MarkupUseCase markupUseCase) {
        this.incomeStatementUseCase = incomeStatementUseCase;
        this.breakEvenUseCase = breakEvenUseCase;
        this.markupUseCase = markupUseCase;
    }

    @PostMapping("/income-statement")
    @Operation(summary = "Simulate an income statement (DRE) with margins and optional monthly projection",
            description = "Sales deductions are a fraction of gross revenue. In the projection, gross revenue, "
                    + "deductions, COGS and selling expenses grow with revenue; administrative/other expenses, "
                    + "D&A and the financial result stay constant. IR/CSLL applies only when LAIR > 0.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Income statement calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<IncomeStatementResponse> incomeStatement(@Valid @RequestBody IncomeStatementRequest request) {
        IncomeStatementParameters params = new IncomeStatementParameters(
                request.grossRevenue(),
                request.salesDeductionsRate(),
                request.costOfGoodsSold(),
                request.sellingExpenses(),
                request.administrativeExpenses(),
                request.otherOperatingExpenses(),
                request.depreciationAmortization(),
                request.financialRevenue(),
                request.financialExpenses(),
                request.incomeTaxRate(),
                request.months(),
                request.monthlyRevenueGrowth()
        );
        return ResponseEntity.ok(IncomeStatementResponse.from(incomeStatementUseCase.execute(params)));
    }

    @PostMapping("/break-even")
    @Operation(summary = "Calculate accounting, economic and financial break-even points")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Break-even calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Unit price does not exceed unit variable cost")
    })
    public ResponseEntity<BreakEvenResponse> breakEven(@Valid @RequestBody BreakEvenRequest request) {
        BreakEvenParameters params = new BreakEvenParameters(
                request.fixedCosts(),
                request.unitPrice(),
                request.unitVariableCost(),
                request.depreciation(),
                request.desiredProfit(),
                request.currentQuantity()
        );
        return ResponseEntity.ok(BreakEvenResponse.from(breakEvenUseCase.execute(params)));
    }

    @PostMapping("/markup")
    @Operation(summary = "Calculate selling price by markup divisor (rates over selling price)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Markup calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Sum of rates is 100% or more")
    })
    public ResponseEntity<MarkupResponse> markup(@Valid @RequestBody MarkupRequest request) {
        MarkupParameters params = new MarkupParameters(
                request.unitCost(),
                request.salesTaxRate(),
                request.commissionRate(),
                request.cardFeeRate(),
                request.fixedExpenseRate(),
                request.desiredProfitRate(),
                request.extraRates() == null ? null : request.extraRates().stream()
                        .map(e -> new MarkupParameters.NamedRate(e.name(), e.rate()))
                        .toList(),
                request.currentPrice()
        );
        return ResponseEntity.ok(MarkupResponse.from(markupUseCase.execute(params)));
    }
}
