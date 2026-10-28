package com.willembergfilho.ifinance.application.simulation;

import com.willembergfilho.ifinance.domain.simulation.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SnapshotUseCasesTest {

    private static final UUID USER = UUID.randomUUID();

    private SimulationRepository simulations;
    private SnapshotRepository snapshots;
    private Simulation simulation;

    @BeforeEach
    void setUp() {
        simulations = mock(SimulationRepository.class);
        snapshots = mock(SnapshotRepository.class);
        when(snapshots.save(any())).thenAnswer(i -> i.getArgument(0));
        simulation = new Simulation(USER, new SimulationParameters("Casa", AmortizationSystem.SAC,
                new BigDecimal("300000"), new BigDecimal("10.5"), RateType.NOMINAL, 360,
                Periodicity.MONTHLY, false, false, null, List.of()));
        when(simulations.findById(simulation.getId())).thenReturn(Optional.of(simulation));
    }

    @Test
    void createCopiesCurrentParameters() {
        Instant before = Instant.now();

        SimulationSnapshot snapshot = new CreateSnapshotUseCase(simulations, snapshots).execute(USER, simulation.getId());

        assertThat(snapshot.id()).isNotNull();
        assertThat(snapshot.simulationId()).isEqualTo(simulation.getId());
        assertThat(snapshot.parameters()).isEqualTo(simulation.getParameters());
        assertThat(snapshot.snapshotAt()).isAfterOrEqualTo(before);
        verify(snapshots).save(snapshot);
    }

    @Test
    void createForForeignSimulationIsNotFoundAndSavesNothing() {
        assertThatThrownBy(() -> new CreateSnapshotUseCase(simulations, snapshots).execute(UUID.randomUUID(), simulation.getId()))
                .isInstanceOf(SimulationNotFoundException.class);
        verifyNoInteractions(snapshots);
    }

    @Test
    void createForMissingSimulationIsNotFound() {
        UUID missing = UUID.randomUUID();
        when(simulations.findById(missing)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> new CreateSnapshotUseCase(simulations, snapshots).execute(USER, missing))
                .isInstanceOf(SimulationNotFoundException.class);
    }

    @Test
    void listReturnsSnapshotsOfOwnedSimulation() {
        List<SimulationSnapshot> history = List.of(
                new SimulationSnapshot(UUID.randomUUID(), simulation.getId(), simulation.getParameters(), Instant.now()));
        when(snapshots.findBySimulationId(simulation.getId())).thenReturn(history);

        assertThat(new GetSnapshotsUseCase(simulations, snapshots).execute(USER, simulation.getId())).isEqualTo(history);
    }

    @Test
    void listForForeignSimulationIsNotFoundAndDoesNotQuerySnapshots() {
        assertThatThrownBy(() -> new GetSnapshotsUseCase(simulations, snapshots).execute(UUID.randomUUID(), simulation.getId()))
                .isInstanceOf(SimulationNotFoundException.class);
        verifyNoInteractions(snapshots);
    }
}
