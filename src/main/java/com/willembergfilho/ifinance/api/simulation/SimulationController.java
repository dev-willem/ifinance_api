package com.willembergfilho.ifinance.api.simulation;

import com.willembergfilho.ifinance.application.simulation.*;
import com.willembergfilho.ifinance.domain.simulation.Simulation;
import com.willembergfilho.ifinance.domain.simulation.SimulationRepository;
import com.willembergfilho.ifinance.domain.simulation.SimulationSnapshot;
import com.willembergfilho.ifinance.infrastructure.security.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/simulations")
@Tag(name = "simulation")
@SecurityRequirement(name = "oauth2")
public class SimulationController {

    private final RunSimulationUseCase runSimulation;
    private final GetSimulationHistoryUseCase getHistory;
    private final CompareSimulationsUseCase compareSimulations;
    private final PrepaymentUseCase prepayment;
    private final GetSnapshotsUseCase getSnapshots;
    private final CreateSnapshotUseCase createSnapshot;
    private final SimulationRepository simulationRepository;
    private final SimulationRequestMapper mapper;
    private final UserContextHolder userContext;

    public SimulationController(RunSimulationUseCase runSimulation,
                                GetSimulationHistoryUseCase getHistory,
                                CompareSimulationsUseCase compareSimulations,
                                PrepaymentUseCase prepayment,
                                GetSnapshotsUseCase getSnapshots,
                                CreateSnapshotUseCase createSnapshot,
                                SimulationRepository simulationRepository,
                                SimulationRequestMapper mapper,
                                UserContextHolder userContext) {
        this.runSimulation = runSimulation;
        this.getHistory = getHistory;
        this.compareSimulations = compareSimulations;
        this.prepayment = prepayment;
        this.getSnapshots = getSnapshots;
        this.createSnapshot = createSnapshot;
        this.simulationRepository = simulationRepository;
        this.mapper = mapper;
        this.userContext = userContext;
    }

    @PostMapping
    @Operation(summary = "Run a new simulation")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Simulation created"),
            @ApiResponse(responseCode = "400", description = "Validation error"),
            @ApiResponse(responseCode = "422", description = "Calculation error")
    })
    public ResponseEntity<SimulationResponse> create(@Valid @RequestBody SimulationRequest request) {
        UUID userId = userContext.currentUserId();
        Simulation simulation = runSimulation.execute(userId, mapper.toParameters(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(SimulationResponse.from(simulation));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get simulation by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Simulation found"),
            @ApiResponse(responseCode = "404", description = "Not found")
    })
    public ResponseEntity<SimulationResponse> getById(@PathVariable UUID id) {
        UUID userId = userContext.currentUserId();
        Simulation simulation = simulationRepository.findById(id)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> new SimulationNotFoundException(id));
        return ResponseEntity.ok(SimulationResponse.from(simulation));
    }

    @GetMapping("/history")
    @Operation(summary = "List simulation history (paginated)")
    public ResponseEntity<HistoryResponse> history(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID userId = userContext.currentUserId();
        GetSimulationHistoryUseCase.HistoryPage result = getHistory.execute(userId, page, size);
        List<SimulationResponse> items = result.simulations().stream()
                .map(SimulationResponse::from).toList();
        return ResponseEntity.ok(new HistoryResponse(items, result.total(), page, size));
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare up to 5 simulations side by side")
    public ResponseEntity<List<SimulationResponse>> compare(@RequestParam List<UUID> ids) {
        UUID userId = userContext.currentUserId();
        List<Simulation> simulations = compareSimulations.execute(userId, ids);
        return ResponseEntity.ok(simulations.stream().map(SimulationResponse::from).toList());
    }

    @GetMapping("/{id}/prepayment")
    @Operation(summary = "Calculate early repayment value with discount at a given period")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Prepayment calculated"),
            @ApiResponse(responseCode = "400", description = "Period out of range"),
            @ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<PrepaymentResponse> prepayment(
            @PathVariable UUID id,
            @RequestParam int period) {
        UUID userId = userContext.currentUserId();
        return ResponseEntity.ok(PrepaymentResponse.from(prepayment.execute(userId, id, period)));
    }

    @GetMapping("/{id}/snapshots")
    @Operation(summary = "List all parameter snapshots for a simulation")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Snapshots retrieved"),
            @ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<List<SnapshotResponse>> listSnapshots(@PathVariable UUID id) {
        UUID userId = userContext.currentUserId();
        List<SimulationSnapshot> snapshots = getSnapshots.execute(userId, id);
        return ResponseEntity.ok(snapshots.stream().map(SnapshotResponse::from).toList());
    }

    @PostMapping("/{id}/snapshots")
    @Operation(summary = "Save a snapshot of the current simulation parameters")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Snapshot created"),
            @ApiResponse(responseCode = "404", description = "Simulation not found")
    })
    public ResponseEntity<SnapshotResponse> saveSnapshot(@PathVariable UUID id) {
        UUID userId = userContext.currentUserId();
        SimulationSnapshot snapshot = createSnapshot.execute(userId, id);
        return ResponseEntity.status(HttpStatus.CREATED).body(SnapshotResponse.from(snapshot));
    }

    @Schema(name = "SimulationHistoryResponse")
    public record HistoryResponse(List<SimulationResponse> simulations, long total, int page, int size) {}
}
