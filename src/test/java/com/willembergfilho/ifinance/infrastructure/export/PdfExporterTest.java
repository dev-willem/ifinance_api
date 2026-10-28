package com.willembergfilho.ifinance.infrastructure.export;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;

class PdfExporterTest {

    private final PdfExporter exporter = new PdfExporter();

    private record Parsed(int pages, String text) {}

    private Parsed parse(byte[] bytes) throws IOException {
        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(bytes)))) {
            StringBuilder sb = new StringBuilder();
            for (int i = 1; i <= pdf.getNumberOfPages(); i++) {
                sb.append(PdfTextExtractor.getTextFromPage(pdf.getPage(i))).append('\n');
            }
            // pt-BR currency format separates "R$" with a non-breaking space
            return new Parsed(pdf.getNumberOfPages(), sb.toString().replace(' ', ' '));
        }
    }

    @Test
    void producesPdfDocument() {
        byte[] bytes = exporter.export(ExportFixtures.priceSimulation(12));
        assertThat(new String(bytes, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }

    @Test
    void containsSummaryScheduleAndTotals() throws IOException {
        Parsed parsed = parse(exporter.export(ExportFixtures.priceSimulation(12)));

        assertThat(parsed.pages()).isEqualTo(1);
        assertThat(parsed.text())
                .contains("Simulação de Financiamento")
                .contains("Financiamento Carro")
                .contains("PRICE")
                .contains("R$ 10.000,00")
                .contains("1,0000%")          // interestRate 1 (%) -> 0.01 -> 1,0000%
                .contains("EFFECTIVE")
                .contains("12 monthly")
                .contains("Tabela de Amortização")
                .contains("R$ 888,49")
                .contains("R$ 888,47")        // last installment absorbs rounding
                .contains("Total pago: R$ 10.661,86")
                .contains("Total de juros: R$ 661,86")
                .contains("CET (a.a.): 20,4534%");
    }

    @Test
    void longScheduleSpansSeveralPages() throws IOException {
        Parsed parsed = parse(exporter.export(ExportFixtures.priceSimulation(120)));
        assertThat(parsed.pages()).isGreaterThan(1);
        assertThat(parsed.text()).contains("Total pago:");
    }
}
