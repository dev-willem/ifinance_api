package com.willembergfilho.ifinance.infrastructure.export;

import com.willembergfilho.ifinance.domain.index.EconomicIndex;
import com.willembergfilho.ifinance.domain.simulation.*;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.*;

class ExcelExporterTest {

    private final ExcelExporter exporter = new ExcelExporter();

    private XSSFWorkbook open(byte[] bytes) throws IOException {
        return new XSSFWorkbook(new ByteArrayInputStream(bytes));
    }

    @Test
    void writesParametersAndScheduleSheets() throws IOException {
        try (XSSFWorkbook wb = open(exporter.export(ExportFixtures.priceSimulation(12)))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetName(0)).isEqualTo("Parâmetros");
            assertThat(wb.getSheetName(1)).isEqualTo("Tabela de Amortização");
        }
    }

    @Test
    void parametersSheetHasSimulationInputs() throws IOException {
        try (XSSFWorkbook wb = open(exporter.export(ExportFixtures.priceSimulation(12)))) {
            Sheet sheet = wb.getSheet("Parâmetros");
            assertThat(sheet.getRow(0).getCell(0).getStringCellValue()).contains("Parâmetros da Simulação");
            assertThat(sheet.getRow(1).getCell(1).getStringCellValue()).isEqualTo("PRICE");
            assertThat(sheet.getRow(2).getCell(1).getNumericCellValue()).isEqualTo(10000.0);
            assertThat(sheet.getRow(3).getCell(1).getNumericCellValue()).isEqualTo(1.0);
            assertThat(sheet.getRow(5).getCell(1).getNumericCellValue()).isEqualTo(12.0);
            assertThat(sheet.getRow(7).getCell(1).getStringCellValue()).isEqualTo("Sim");
            assertThat(sheet.getRow(8).getCell(1).getStringCellValue()).isEqualTo("Não");
        }
    }

    @Test
    void scheduleSheetHasHeaderOneRowPerInstallmentAndTotals() throws IOException {
        try (XSSFWorkbook wb = open(exporter.export(ExportFixtures.priceSimulation(12)))) {
            Sheet sheet = wb.getSheet("Tabela de Amortização");
            Row header = sheet.getRow(0);
            List<String> headers = IntStream.range(0, 7)
                    .mapToObj(i -> header.getCell(i).getStringCellValue()).toList();
            assertThat(headers).containsExactly("#", "Saldo Anterior", "Amortização", "Juros",
                    "Encargos", "Parcela", "Saldo Após");

            for (int period = 1; period <= 12; period++) {
                assertThat(sheet.getRow(period).getCell(0).getNumericCellValue()).isEqualTo(period);
            }
            // period 1: balance 10000, interest 1% = 100.00, amortization 888.49 - 100.00 = 788.49
            Row first = sheet.getRow(1);
            assertThat(first.getCell(1).getNumericCellValue()).isEqualTo(10000.0);
            assertThat(first.getCell(2).getNumericCellValue()).isEqualTo(788.49);
            assertThat(first.getCell(3).getNumericCellValue()).isEqualTo(100.0);
            assertThat(first.getCell(5).getNumericCellValue()).isEqualTo(888.49);
            assertThat(sheet.getRow(12).getCell(6).getNumericCellValue()).isZero();
            assertThat(sheet.getRow(13)).isNull();

            Row totals = sheet.getRow(14);
            assertThat(totals.getCell(0).getStringCellValue()).isEqualTo("TOTAIS");
            assertThat(totals.getCell(5).getNumericCellValue()).isEqualTo(10661.86);
            assertThat(totals.getCell(3).getNumericCellValue()).isEqualTo(661.86);
        }
    }

    @Test
    void simulationWithoutScheduleOnlyHasParametersSheet() throws IOException {
        Simulation draft = new Simulation(UUID.randomUUID(), ExportFixtures.priceParams(12));
        try (XSSFWorkbook wb = open(exporter.export(draft))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(1);
        }
    }

    @Test
    void monetaryCorrectionShowsIndexName() throws IOException {
        SimulationParameters p = ExportFixtures.priceParams(3);
        SimulationParameters withIpca = new SimulationParameters(p.name(), p.amortizationSystem(), p.principal(),
                p.interestRate(), p.rateType(), p.term(), p.periodicity(), false, true, EconomicIndex.IPCA, List.of());
        try (XSSFWorkbook wb = open(exporter.export(new Simulation(UUID.randomUUID(), withIpca)))) {
            assertThat(wb.getSheet("Parâmetros").getRow(7).getCell(1).getStringCellValue()).isEqualTo("Não");
            assertThat(wb.getSheet("Parâmetros").getRow(8).getCell(1).getStringCellValue()).isEqualTo("IPCA");
        }
    }

    @Test
    void correctionEnabledWithoutIndexDoesNotFail() {
        SimulationParameters p = new SimulationParameters("x", AmortizationSystem.SAC, BigDecimal.TEN,
                BigDecimal.ONE, RateType.EFFECTIVE, 1, Periodicity.MONTHLY, false, true, null, List.of());
        assertThatCode(() -> exporter.export(new Simulation(UUID.randomUUID(), p))).doesNotThrowAnyException();
    }
}
