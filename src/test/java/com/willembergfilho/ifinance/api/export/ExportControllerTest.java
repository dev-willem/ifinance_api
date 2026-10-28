package com.willembergfilho.ifinance.api.export;

import com.willembergfilho.ifinance.api.advice.GlobalExceptionHandler;
import com.willembergfilho.ifinance.application.simulation.ExportSimulationUseCase;
import com.willembergfilho.ifinance.domain.export.SimulationExporter;
import com.willembergfilho.ifinance.domain.simulation.*;
import com.willembergfilho.ifinance.infrastructure.security.UserContextHolder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real ExportSimulationUseCase (it decides media type and filename); only ports are mocked.
class ExportControllerTest {

    private static final UUID USER = UUID.randomUUID();
    private static final byte[] PDF = "%PDF-1.7 fake".getBytes();
    private static final byte[] XLSX = {0x50, 0x4B, 0x03, 0x04};

    private SimulationRepository repo;
    private SimulationExporter exporter;
    private MockMvc mvc;
    private Simulation simulation;

    @BeforeEach
    void setUp() {
        repo = mock(SimulationRepository.class);
        exporter = mock(SimulationExporter.class);
        UserContextHolder userContext = mock(UserContextHolder.class);
        when(userContext.currentUserId()).thenReturn(USER);
        when(exporter.exportPdf(any())).thenReturn(PDF);
        when(exporter.exportExcel(any())).thenReturn(XLSX);

        simulation = new Simulation(USER, new SimulationParameters("s", AmortizationSystem.SAC, BigDecimal.TEN,
                BigDecimal.ONE, RateType.EFFECTIVE, 1, Periodicity.MONTHLY, false, false, null, List.of()));
        when(repo.findById(simulation.getId())).thenReturn(Optional.of(simulation));

        mvc = MockMvcBuilders.standaloneSetup(
                        new ExportController(new ExportSimulationUseCase(repo, exporter), userContext))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void defaultsToPdfAttachment() throws Exception {
        mvc.perform(get("/api/v1/simulations/{id}/export", simulation.getId()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"simulacao-" + simulation.getId() + ".pdf\""))
                .andExpect(content().bytes(PDF));
        verify(repo).save(simulation);
        assertThat(simulation.getStatus()).isEqualTo(SimulationStatus.EXPORTED);
    }

    @Test
    void xlsxFormatIsCaseInsensitive() throws Exception {
        mvc.perform(get("/api/v1/simulations/{id}/export", simulation.getId()).param("format", "XLSX"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"simulacao-" + simulation.getId() + ".xlsx\""))
                .andExpect(content().bytes(XLSX));
    }

    @Test
    void unknownSimulationIs404() throws Exception {
        UUID missing = UUID.randomUUID();
        when(repo.findById(missing)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/simulations/{id}/export", missing))
                .andExpect(status().isNotFound());
        verifyNoInteractions(exporter);
    }

    @Test
    void someoneElsesSimulationIs404() throws Exception {
        Simulation foreign = new Simulation(UUID.randomUUID(), simulation.getParameters());
        when(repo.findById(foreign.getId())).thenReturn(Optional.of(foreign));

        mvc.perform(get("/api/v1/simulations/{id}/export", foreign.getId()))
                .andExpect(status().isNotFound());
        verifyNoInteractions(exporter);
    }

    @Test
    void unsupportedFormatIs400() throws Exception {
        mvc.perform(get("/api/v1/simulations/{id}/export", simulation.getId()).param("format", "docx"))
                .andExpect(status().isBadRequest());
        verify(repo, never()).save(any());
    }

    @Test
    void malformedIdIs400() throws Exception {
        mvc.perform(get("/api/v1/simulations/not-a-uuid/export"))
                .andExpect(status().isBadRequest());
    }
}
