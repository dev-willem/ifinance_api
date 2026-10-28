package com.willembergfilho.ifinance.api.labor;

import com.willembergfilho.ifinance.application.labor.LaborCalculationUseCase;
import com.willembergfilho.ifinance.domain.labor.NetSalaryParameters;
import com.willembergfilho.ifinance.domain.labor.TerminationParameters;
import com.willembergfilho.ifinance.domain.labor.ThirteenthSalaryParameters;
import com.willembergfilho.ifinance.domain.labor.VacationParameters;
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

import static java.util.Objects.requireNonNullElse;

@RestController
@RequestMapping("/api/v1/labor")
@Tag(name = "labor")
@SecurityRequirement(name = "oauth2")
public class LaborController {

    private final LaborCalculationUseCase useCase;

    public LaborController(LaborCalculationUseCase useCase) {
        this.useCase = useCase;
    }

    @PostMapping("/net-salary")
    @Operation(summary = "Calculate CLT net salary (INSS, IRRF with Lei 15.270/2025 reduction, FGTS, employer cost)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Net salary calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Deductions exceed the gross salary")
    })
    public ResponseEntity<NetSalaryResponse> netSalary(@Valid @RequestBody NetSalaryRequest request) {
        NetSalaryParameters params = new NetSalaryParameters(
                request.grossSalary(),
                requireNonNullElse(request.dependents(), 0),
                request.alimony(),
                request.otherDiscounts(),
                request.employerInssRate(),
                request.year()
        );
        return ResponseEntity.ok(NetSalaryResponse.from(useCase.netSalary(params)));
    }

    @PostMapping("/vacation")
    @Operation(summary = "Calculate vacation pay with 1/3, abono pecuniário, INSS and IRRF")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Vacation calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<VacationResponse> vacation(@Valid @RequestBody VacationRequest request) {
        VacationParameters params = new VacationParameters(
                request.monthlySalary(),
                request.averageVariablePay(),
                request.days(),
                requireNonNullElse(request.soldDays(), 0),
                requireNonNullElse(request.dependents(), 0),
                request.year()
        );
        return ResponseEntity.ok(VacationResponse.from(useCase.vacation(params)));
    }

    @PostMapping("/thirteenth")
    @Operation(summary = "Calculate 13th salary installments with INSS and exclusive IRRF")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "13th salary calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<ThirteenthResponse> thirteenth(@Valid @RequestBody ThirteenthRequest request) {
        ThirteenthSalaryParameters params = new ThirteenthSalaryParameters(
                request.monthlySalary(),
                request.averageVariablePay(),
                request.monthsWorked(),
                requireNonNullElse(request.dependents(), 0),
                request.year()
        );
        return ResponseEntity.ok(ThirteenthResponse.from(useCase.thirteenth(params)));
    }

    @PostMapping("/termination")
    @Operation(summary = "Calculate CLT termination items (rescisão), FGTS fine and withdrawal")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Termination calculated"),
            @ApiResponse(responseCode = "400", description = "Validation error")
    })
    public ResponseEntity<TerminationResponse> termination(@Valid @RequestBody TerminationRequest request) {
        TerminationParameters params = new TerminationParameters(
                request.monthlySalary(),
                request.admissionDate(),
                request.terminationDate(),
                request.terminationType(),
                request.noticeType(),
                requireNonNullElse(request.expiredVacationPeriods(), 0),
                request.fgtsBalance(),
                requireNonNullElse(request.dependents(), 0),
                request.year()
        );
        return ResponseEntity.ok(TerminationResponse.from(useCase.termination(params)));
    }
}
