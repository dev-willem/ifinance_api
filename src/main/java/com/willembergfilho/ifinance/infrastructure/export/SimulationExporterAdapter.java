package com.willembergfilho.ifinance.infrastructure.export;

import com.willembergfilho.ifinance.domain.export.SimulationExporter;
import com.willembergfilho.ifinance.domain.simulation.Simulation;
import org.springframework.stereotype.Component;

@Component
public class SimulationExporterAdapter implements SimulationExporter {

    private final PdfExporter pdfExporter;
    private final ExcelExporter excelExporter;

    public SimulationExporterAdapter(PdfExporter pdfExporter, ExcelExporter excelExporter) {
        this.pdfExporter = pdfExporter;
        this.excelExporter = excelExporter;
    }

    @Override
    public byte[] exportPdf(Simulation simulation) {
        return pdfExporter.export(simulation);
    }

    @Override
    public byte[] exportExcel(Simulation simulation) {
        return excelExporter.export(simulation);
    }
}
