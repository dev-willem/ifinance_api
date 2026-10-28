package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.simulation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

class CompareSimulationsUseCaseTest {

    private static final UUID USER = UUID.randomUUID();

    private SimulationRepository repo;
    private CompareSimulationsUseCase useCase;
    private List<Simulation> stored;

    @BeforeEach
    void setUp() {
        repo = mock(SimulationRepository.class);
        stored = new java.util.ArrayList<>();
        // mimics JPA findAllById: each existing id once, regardless of duplicates in the request
        when(repo.findAllById(anyList())).thenAnswer(inv -> {
            Collection<UUID> ids = inv.getArgument(0);
            return stored.stream().filter(s -> ids.contains(s.getId())).toList();
        });
        useCase = new CompareSimulationsUseCase(repo);
    }

    private Simulation simulationOf(UUID owner) {
        Simulation s = new Simulation(owner, new SimulationParameters("s", AmortizationSystem.SAC, BigDecimal.TEN,
                BigDecimal.ONE, RateType.EFFECTIVE, 1, Periodicity.MONTHLY, false, false, null, List.of()));
        stored.add(s);
        return s;
    }

    @Test
    void returnsOwnedSimulations() {
        Simulation a = simulationOf(USER);
        Simulation b = simulationOf(USER);

        assertThat(useCase.execute(USER, List.of(a.getId(), b.getId()))).containsExactlyInAnyOrder(a, b);
    }

    @Test
    void acceptsUpToFiveSimulations() {
        List<UUID> ids = java.util.stream.Stream.generate(() -> simulationOf(USER).getId()).limit(5).toList();

        assertThat(useCase.execute(USER, ids)).hasSize(5);
    }

    @Test
    void rejectsMoreThanFive() {
        List<UUID> ids = java.util.stream.Stream.generate(UUID::randomUUID).limit(6).toList();

        assertThatThrownBy(() -> useCase.execute(USER, ids)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(repo);
    }

    @Test
    void rejectsNullOrEmpty() {
        assertThatThrownBy(() -> useCase.execute(USER, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.execute(USER, List.of())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void foreignSimulationIsReportedAsNotFound() {
        Simulation mine = simulationOf(USER);
        Simulation foreign = simulationOf(UUID.randomUUID());

        assertThatThrownBy(() -> useCase.execute(USER, List.of(mine.getId(), foreign.getId())))
                .isInstanceOf(SimulationNotFoundException.class)
                .hasMessageContaining(foreign.getId().toString());
    }

    @Test
    void missingSimulationIsReportedAsNotFound() {
        Simulation mine = simulationOf(USER);
        UUID missing = UUID.randomUUID();

        assertThatThrownBy(() -> useCase.execute(USER, List.of(mine.getId(), missing)))
                .isInstanceOf(SimulationNotFoundException.class)
                .hasMessageContaining(missing.toString());
    }

    @Test
    void duplicateIdsDoNotCrash() {
        // regression: duplicates made owned.size() < ids.size() with no missing id -> NoSuchElementException (500)
        Simulation a = simulationOf(USER);

        assertThat(useCase.execute(USER, List.of(a.getId(), a.getId()))).containsExactly(a);
    }
}
