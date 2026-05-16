package com.willembergfilho.ifinance.infrastructure.export;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.willembergfilho.ifinance.domain.simulation.Installment;
import com.willembergfilho.ifinance.domain.simulation.Simulation;
import com.willembergfilho.ifinance.domain.simulation.SimulationParameters;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Component
public class PdfExporter {

    private static final NumberFormat CURRENCY = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    private static final NumberFormat PERCENT = NumberFormat.getPercentInstance(new Locale("pt", "BR"));

    static {
        PERCENT.setMinimumFractionDigits(4);
        PERCENT.setMaximumFractionDigits(4);
    }

    public byte[] export(Simulation simulation) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        try {
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

            PdfDocument pdf = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdf);

            SimulationParameters p = simulation.getParameters();

            doc.add(new Paragraph("iFinance — Simulação de Financiamento")
                    .setFont(bold).setFontSize(16).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(p.name() != null ? p.name() : "Simulação")
                    .setFont(regular).setFontSize(12).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(" "));

            doc.add(summaryTable(simulation, bold, regular));
            doc.add(new Paragraph(" "));

            if (simulation.getSchedule() != null) {
                doc.add(new Paragraph("Tabela de Amortização").setFont(bold).setFontSize(13));
                doc.add(amortizationTable(simulation.getSchedule().installments(), bold, regular));
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Total pago: " + currency(simulation.getSchedule().totalPaid()))
                        .setFont(bold));
                doc.add(new Paragraph("Total de juros: " + currency(simulation.getSchedule().totalInterest()))
                        .setFont(bold));
                if (simulation.getSchedule().cetResult() != null) {
                    doc.add(new Paragraph("CET (a.a.): " + percent(simulation.getSchedule().cetResult().annualRate()))
                            .setFont(bold));
                }
            }

            doc.close();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF.", e);
        }

        return baos.toByteArray();
    }

    private Table summaryTable(Simulation simulation, PdfFont bold, PdfFont regular) {
        SimulationParameters p = simulation.getParameters();
        Table table = new Table(UnitValue.createPercentArray(new float[]{40, 60})).useAllAvailableWidth();

        addRow(table, "Sistema de Amortização", p.amortizationSystem().name(), bold, regular);
        addRow(table, "Valor Financiado", currency(p.principal()), bold, regular);
        addRow(table, "Taxa de Juros", percent(p.interestRate()) + " (" + p.rateType().name() + ")", bold, regular);
        addRow(table, "Prazo", p.term() + " " + p.periodicity().name().toLowerCase(), bold, regular);
        if (p.cetEnabled()) addRow(table, "CET habilitado", "Sim", bold, regular);
        if (p.inflationCorrectionEnabled())
            addRow(table, "Correção Monetária", p.inflationIndex().name(), bold, regular);

        return table;
    }

    private Table amortizationTable(List<Installment> installments, PdfFont bold, PdfFont regular) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{5, 16, 16, 16, 16, 15, 16}))
                .useAllAvailableWidth().setFontSize(8);

        String[] headers = {"#", "Saldo Anterior", "Amortização", "Juros", "Encargos", "Parcela", "Saldo Após"};
        for (String h : headers) {
            table.addHeaderCell(new Cell().add(new Paragraph(h).setFont(bold))
                    .setBackgroundColor(ColorConstants.LIGHT_GRAY));
        }

        for (Installment inst : installments) {
            table.addCell(new Paragraph(String.valueOf(inst.periodNumber())).setFont(regular));
            table.addCell(new Paragraph(currency(inst.principalBalanceBefore())).setFont(regular));
            table.addCell(new Paragraph(currency(inst.amortization())).setFont(regular));
            table.addCell(new Paragraph(currency(inst.interest())).setFont(regular));
            table.addCell(new Paragraph(currency(inst.additionalCharges())).setFont(regular));
            table.addCell(new Paragraph(currency(inst.total())).setFont(regular));
            table.addCell(new Paragraph(currency(inst.principalBalanceAfter())).setFont(regular));
        }
        return table;
    }

    private void addRow(Table table, String label, String value, PdfFont bold, PdfFont regular) {
        table.addCell(new Cell().add(new Paragraph(label).setFont(bold)));
        table.addCell(new Cell().add(new Paragraph(value).setFont(regular)));
    }

    private String currency(BigDecimal value) {
        return value != null ? CURRENCY.format(value) : "-";
    }

    private String percent(BigDecimal value) {
        return value != null ? PERCENT.format(value) : "-";
    }
}
