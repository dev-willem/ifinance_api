package com.willembergfilho.ifinance.api.index;

import com.willembergfilho.ifinance.application.index.FetchIndexRateUseCase;
import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.index.IndexRate;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/indexes")
@Tag(name = "index")
@SecurityRequirement(name = "oauth2")
public class IndexController {

    private final FetchIndexRateUseCase fetchIndexRate;

    public IndexController(FetchIndexRateUseCase fetchIndexRate) {
        this.fetchIndexRate = fetchIndexRate;
    }

    @GetMapping("/{index}/current")
    @Operation(summary = "Get the latest available rate for an economic index")
    public ResponseEntity<IndexRateResponse> current(@PathVariable EconomicIndex index) {
        return fetchIndexRate.fetchCurrent(index)
                .map(IndexRateResponse::from)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{index}")
    @Operation(summary = "Get rates for an economic index within a date range")
    public ResponseEntity<List<IndexRateResponse>> range(
            @PathVariable EconomicIndex index,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        List<IndexRate> rates = fetchIndexRate.fetchRange(index, from, to);
        return ResponseEntity.ok(rates.stream().map(IndexRateResponse::from).toList());
    }

    public record IndexRateResponse(
            String index,
            LocalDate referenceDate,
            BigDecimal annualRate,
            boolean isProjection
    ) {
        public static IndexRateResponse from(IndexRate indexRate) {
            return new IndexRateResponse(
                    indexRate.index().name(),
                    indexRate.referenceDate(),
                    annualize(indexRate),
                    indexRate.isProjection()
            );
        }

        /**
         * Converte a taxa bruta do BCB para percentual anual padronizado.
         * CDI/SELIC: taxa diária em % → (1+r/100)^252 − 1
         * IPCA/TR/IGP-M: taxa mensal em % → (1+r/100)^12 − 1
         * Resultado: percentual anual com 2 casas decimais (ex: 14,37).
         */
        private static BigDecimal annualize(IndexRate indexRate) {
            double rawDecimal = indexRate.rate().doubleValue() / 100.0;
            double annualDecimal = switch (indexRate.index().getPeriodicity()) {
                case DAILY -> Math.pow(1.0 + rawDecimal, 252) - 1.0;
                case MONTHLY -> Math.pow(1.0 + rawDecimal, 12) - 1.0;
            };
            return BigDecimal.valueOf(annualDecimal * 100.0)
                    .setScale(2, RoundingMode.HALF_EVEN);
        }
    }
}
