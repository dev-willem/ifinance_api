package com.willembergfilho.ifinance.api.export;

import com.willembergfilho.ifinance.application.simulation.ExportSimulationUseCase;
import com.willembergfilho.ifinance.infrastructure.security.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "export")
@SecurityRequirement(name = "oauth2")
public class ExportController {

    private final ExportSimulationUseCase exportUseCase;
    private final UserContextHolder userContext;

    public ExportController(ExportSimulationUseCase exportUseCase, UserContextHolder userContext) {
        this.exportUseCase = exportUseCase;
        this.userContext = userContext;
    }

    @GetMapping("/{id}/export")
    @Operation(summary = "Export simulation as PDF or Excel")
    public ResponseEntity<byte[]> export(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "pdf") String format) {

        UUID userId = userContext.currentUserId();
        ExportSimulationUseCase.ExportResult result = exportUseCase.execute(userId, id, format);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(result.mediaType()));
        headers.setContentDisposition(
                ContentDisposition.attachment().filename(result.filename()).build());

        return ResponseEntity.ok().headers(headers).body(result.content());
    }
}
