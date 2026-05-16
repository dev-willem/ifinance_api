package com.willembergfilho.ifinance.domain.export;

import com.willembergfilho.ifinance.domain.simulation.Simulation;

public interface SimulationExporter {

    byte[] exportPdf(Simulation simulation);

    byte[] exportExcel(Simulation simulation);
}
