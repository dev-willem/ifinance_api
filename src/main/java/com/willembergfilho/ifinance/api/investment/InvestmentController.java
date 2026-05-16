package com.willembergfilho.ifinance.api.investment;

import com.willembergfilho.ifinance.application.investment.*;
import com.willembergfilho.ifinance.domain.investment.Investment;
import com.willembergfilho.ifinance.domain.investment.InvestmentParameters;
import com.willembergfilho.ifinance.domain.investment.InvestmentRepository;
import com.willembergfilho.ifinance.domain.investment.InvestmentResult;
import com.willembergfilho.ifinance.infrastructure.security.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/investments")
@Tag(name = "investment")
@SecurityRequirement(name = "oauth2")
public class InvestmentController {

    private final RunInvestmentUseCase runInvestment;
    private final GetInvestmentHistoryUseCase getHistory;
    private final CompareInvestmentsUseCase compareInvestments;
    private final DirectCompareInvestmentsUseCase directCompare;
    private final InvestmentRepository investmentRepository;
    private final InvestmentRequestMapper mapper;
    private final UserContextHolder userContext;

    public InvestmentController(RunInvestmentUseCase runInvestment,
                                GetInvestmentHistoryUseCase getHistory,
                                CompareInvestmentsUseCase compareInvestments,
                                DirectCompareInvestmentsUseCase directCompare,
                                InvestmentRepository investmentRepository,
                                InvestmentRequestMapper mapper,
                                UserContextHolder userContext) {
        this.runInvestment = runInvestment;
        this.getHistory = getHistory;
        this.compareInvestments = compareInvestments;
        this.directCompare = directCompare;
        this.investmentRepository = investmentRepository;
        this.mapper = mapper;
        this.userContext = userContext;
    }

    @PostMapping
    @Operation(summary = "Run a new investment calculation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Investment calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Calculation error")
    })
    public ResponseEntity<InvestmentResponse> create(@Valid @RequestBody InvestmentRequest request) {
        UUID userId = userContext.currentUserId();
        Investment investment = runInvestment.execute(userId, mapper.toParameters(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(InvestmentResponse.from(investment));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get investment by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Investment found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<InvestmentResponse> getById(@PathVariable UUID id) {
        UUID userId = userContext.currentUserId();
        Investment investment = investmentRepository.findById(id)
                .filter(i -> i.getUserId().equals(userId))
                .orElseThrow(() -> new InvestmentNotFoundException(id));
        return ResponseEntity.ok(InvestmentResponse.from(investment));
    }

    @GetMapping("/history")
    @Operation(summary = "List investment history (paginated)")
    public ResponseEntity<HistoryResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userContext.currentUserId();
        GetInvestmentHistoryUseCase.HistoryPage result = getHistory.execute(userId, page, size);
        List<InvestmentResponse> items = result.investments().stream()
                .map(InvestmentResponse::from).toList();
        return ResponseEntity.ok(new HistoryResponse(items, result.total(), page, size));
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare up to 5 saved investments side by side")
    public ResponseEntity<List<InvestmentResponse>> compare(@RequestParam List<UUID> ids) {
        UUID userId = userContext.currentUserId();
        List<Investment> investments = compareInvestments.execute(userId, ids);
        return ResponseEntity.ok(investments.stream().map(InvestmentResponse::from).toList());
    }

    @PostMapping("/compare-direct")
    @Operation(summary = "Compare up to 5 investment products directly without saving")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Direct comparison calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Calculation error")
    })
    public ResponseEntity<List<DirectCompareResponse>> compareDirect(
            @Valid @RequestBody DirectCompareRequest request) {
        List<InvestmentParameters> params = request.investments().stream()
                .map(mapper::toParameters)
                .toList();
        List<InvestmentResult> results = directCompare.execute(params);

        List<DirectCompareResponse> responses = new java.util.ArrayList<>();
        for (int i = 0; i < params.size(); i++) {
            InvestmentParameters p = params.get(i);
            InvestmentResult r = results.get(i);
            BigDecimal irRatePct = r.irRate() != null
                    ? r.irRate().multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_EVEN) : null;
            BigDecimal grossPct = r.grossAnnualRate() != null
                    ? r.grossAnnualRate().multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_EVEN) : null;
            BigDecimal netPct = r.netAnnualRate() != null
                    ? r.netAnnualRate().multiply(new BigDecimal("100")).setScale(4, RoundingMode.HALF_EVEN) : null;
            responses.add(new DirectCompareResponse(
                    p.name(), p.type(), p.rateBasis(), p.rateValue(), p.principal(), p.termDays(),
                    r.grossReturn(), r.netReturn(), r.irAmount(), irRatePct, grossPct, netPct,
                    r.indexRateUsed(), p.type().isTaxExempt()
            ));
        }
        return ResponseEntity.ok(responses);
    }

    @Schema(name = "InvestmentHistoryResponse")
    public record HistoryResponse(List<InvestmentResponse> investments, long total, int page, int size) {}

    public record DirectCompareRequest(
            @jakarta.validation.Valid
            @jakarta.validation.constraints.NotEmpty
            @jakarta.validation.constraints.Size(min = 2, max = 5)
            List<@Valid InvestmentRequest> investments
    ) {}

    public record DirectCompareResponse(
            String name,
            com.willembergfilho.ifinance.domain.investment.InvestmentType investmentType,
            com.willembergfilho.ifinance.domain.investment.RateBasis rateBasis,
            BigDecimal rateValue,
            BigDecimal principal,
            int termDays,
            BigDecimal grossReturn,
            BigDecimal netReturn,
            BigDecimal irAmount,
            BigDecimal irRate,
            BigDecimal grossAnnualRate,
            BigDecimal netAnnualRate,
            BigDecimal indexRateUsed,
            boolean isTaxExempt
    ) {}
}
