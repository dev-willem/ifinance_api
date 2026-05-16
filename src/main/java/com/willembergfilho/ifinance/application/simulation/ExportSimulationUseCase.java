package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.export.SimulationExporter;
import com.willembergfilho.ifinance.domain.simulation.Simulation;
import com.willembergfilho.ifinance.domain.simulation.SimulationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ExportSimulationUseCase {

    private final SimulationRepository simulationRepository;
    private final SimulationExporter exporter;

    public ExportSimulationUseCase(SimulationRepository simulationRepository, SimulationExporter exporter) {
        this.simulationRepository = simulationRepository;
        this.exporter = exporter;
    }

    public record ExportResult(byte[] content, String mediaType, String filename) {}

    @Transactional
    public ExportResult execute(UUID userId, UUID simulationId, String format) {
        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new SimulationNotFoundException(simulationId));

        if (!simulation.getUserId().equals(userId)) {
            throw new SecurityException("Simulation does not belong to the authenticated user.");
        }

        return switch (format.toLowerCase()) {
            case "pdf" -> {
                byte[] content = exporter.exportPdf(simulation);
                simulation.markExported();
                simulationRepository.save(simulation);
                yield new ExportResult(content, "application/pdf",
                        "simulacao-" + simulationId + ".pdf");
            }
            case "xlsx" -> {
                byte[] content = exporter.exportExcel(simulation);
                simulation.markExported();
                simulationRepository.save(simulation);
                yield new ExportResult(content, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "simulacao-" + simulationId + ".xlsx");
            }
            default -> throw new IllegalArgumentException("Unsupported export format: " + format + ". Use 'pdf' or 'xlsx'.");
        };
    }
}
