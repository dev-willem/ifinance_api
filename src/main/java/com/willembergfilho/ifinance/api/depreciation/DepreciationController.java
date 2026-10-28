package com.willembergfilho.ifinance.api.depreciation;

import com.willembergfilho.ifinance.application.depreciation.DepreciationUseCase;
import com.willembergfilho.ifinance.domain.depreciation.DepreciationMethod;
import com.willembergfilho.ifinance.domain.depreciation.DepreciationParameters;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/v1/business/depreciation")
@Tag(name = "depreciation")
@SecurityRequirement(name = "oauth2")
public class DepreciationController {

    private final DepreciationUseCase useCase;

    public DepreciationController(DepreciationUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping
    @Operation(summary = "Calculate an asset depreciation schedule",
            description = "Straight-line, sum-of-years-digits, declining balance (switches to straight-line "
                    + "when that yields a higher charge) or units of production. shiftsPerDay applies the "
                    + "RIR/2018 art. 323 coefficients (1.0 / 1.5 / 2.0); firstYearMonths pro-rates the first "
                    + "fiscal year (ignored for units of production, whose unitsPerYear already reflect actual "
                    + "output). annualRatePercent is the first-period rate: on the depreciable base for "
                    + "straight-line, sum-of-years-digits and units of production; on the book value for "
                    + "declining balance.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Depreciation schedule calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<DepreciationResponse> calculate(@Valid @RequestBody DepreciationRequest request) {
        DepreciationParameters params = new DepreciationParameters(
                request.assetValue(),
                request.residualValue(),
                request.usefulLifeYears(),
                request.method(),
                request.decliningFactor(),
                request.totalUnits(),
                request.unitsPerYear(),
                Objects.requireNonNullElse(request.shiftsPerDay(), 1),
                Objects.requireNonNullElse(request.firstYearMonths(), 12)
        );
        return ResponseEntity.ok(DepreciationResponse.from(useCase.execute(params)));
    }

    @PostMapping("/compare")
    @Operation(summary = "Compare straight-line, sum-of-years-digits and declining balance schedules")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Schedules calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<List<DepreciationResponse>> compare(
            @Valid @RequestBody DepreciationCompareRequest request) {
        DepreciationParameters params = new DepreciationParameters(
                request.assetValue(),
                request.residualValue(),
                request.usefulLifeYears(),
                DepreciationMethod.STRAIGHT_LINE,
                request.decliningFactor(),
                null,
                null,
                Objects.requireNonNullElse(request.shiftsPerDay(), 1),
                Objects.requireNonNullElse(request.firstYearMonths(), 12)
        );
        return ResponseEntity.ok(useCase.compare(params).stream().map(DepreciationResponse::from).toList());
    }

    @GetMapping("/asset-classes")
    @Operation(summary = "List Receita Federal reference useful lives and rates (IN RFB 1.700/2017, Anexo III)")
    @ApiResponse(responseCode = "200", description = "Asset classes listed")
    public ResponseEntity<List<AssetClassResponse>> assetClasses() {
        return ResponseEntity.ok(useCase.assetClasses().stream().map(AssetClassResponse::from).toList());
    }
}
