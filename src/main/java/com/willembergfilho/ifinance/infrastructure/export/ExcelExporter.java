package com.willembergfilho.ifinance.infrastructure.export;

import com.willembergfilho.ifinance.domain.simulation.Installment;
import com.willembergfilho.ifinance.domain.simulation.Simulation;
import com.willembergfilho.ifinance.domain.simulation.SimulationParameters;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;

@Component
public class ExcelExporter {

    public byte[] export(Simulation simulation) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle currencyStyle = createCurrencyStyle(workbook);

            writeParametersSheet(workbook, simulation.getParameters(), headerStyle);

            if (simulation.getSchedule() != null) {
                writeScheduleSheet(workbook, simulation.getSchedule().installments(),
                        simulation.getSchedule().totalPaid(),
                        simulation.getSchedule().totalInterest(),
                        headerStyle, currencyStyle);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate Excel export.", e);
        }
    }

    private void writeParametersSheet(XSSFWorkbook workbook, SimulationParameters p, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Parâmetros");
        int row = 0;

        Row title = sheet.createRow(row++);
        Cell titleCell = title.createCell(0);
        titleCell.setCellValue("iFinance — Parâmetros da Simulação");
        titleCell.setCellStyle(headerStyle);

        Object[][] params = {
                {"Sistema de Amortização", p.amortizationSystem().name()},
                {"Valor Financiado", p.principal().doubleValue()},
                {"Taxa de Juros", p.interestRate().doubleValue()},
                {"Tipo de Taxa", p.rateType().name()},
                {"Prazo", (double) p.term()},
                {"Periodicidade", p.periodicity().name()},
                {"CET Habilitado", p.cetEnabled() ? "Sim" : "Não"},
                {"Correção Monetária", p.inflationCorrectionEnabled() ? p.inflationIndex().name() : "Não"}
        };

        for (Object[] param : params) {
            Row dataRow = sheet.createRow(row++);
            dataRow.createCell(0).setCellValue((String) param[0]);
            Cell valueCell = dataRow.createCell(1);
            if (param[1] instanceof Double d) {
                valueCell.setCellValue(d);
            } else {
                valueCell.setCellValue(param[1].toString());
            }
        }
        sheet.autoSizeColumn(0);
        sheet.autoSizeColumn(1);
    }

    private void writeScheduleSheet(XSSFWorkbook workbook, List<Installment> installments,
                                    BigDecimal totalPaid, BigDecimal totalInterest,
                                    CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Tabela de Amortização");

        String[] headers = {"#", "Saldo Anterior", "Amortização", "Juros", "Encargos", "Parcela", "Saldo Após"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        int rowNum = 1;
        for (Installment inst : installments) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(inst.periodNumber());
            setCurrency(row.createCell(1), inst.principalBalanceBefore(), currencyStyle);
            setCurrency(row.createCell(2), inst.amortization(), currencyStyle);
            setCurrency(row.createCell(3), inst.interest(), currencyStyle);
            setCurrency(row.createCell(4), inst.additionalCharges(), currencyStyle);
            setCurrency(row.createCell(5), inst.total(), currencyStyle);
            setCurrency(row.createCell(6), inst.principalBalanceAfter(), currencyStyle);
        }

        Row totalsRow = sheet.createRow(rowNum + 1);
        totalsRow.createCell(0).setCellValue("TOTAIS");
        setCurrency(totalsRow.createCell(5), totalPaid, currencyStyle);
        setCurrency(totalsRow.createCell(3), totalInterest, currencyStyle);

        for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
    }

    private void setCurrency(Cell cell, BigDecimal value, CellStyle style) {
        if (value != null) {
            cell.setCellValue(value.doubleValue());
            cell.setCellStyle(style);
        }
    }

    private CellStyle createHeaderStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle createCurrencyStyle(XSSFWorkbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("\"R$\"#,##0.00"));
        return style;
    }
}
