-- Versionamento de simulações para comparação de cenários.
-- O estado completo é armazenado como jsonb — imutável após criação.
-- Não duplica colunas de simulations; o snapshot é uma fotografia histórica.
CREATE TABLE simulation_snapshots (
    id                  UUID  PRIMARY KEY DEFAULT gen_random_uuid(),
    simulation_id       UUID  NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
    parameters_snapshot JSONB NOT NULL,
    snapshot_at         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_snapshots_simulation_id ON simulation_snapshots (simulation_id);
CREATE INDEX idx_snapshots_snapshot_at   ON simulation_snapshots (simulation_id, snapshot_at DESC);

-- Índice GIN para consultas dentro do jsonb (ex: filtrar por amortization_system)
CREATE INDEX idx_snapshots_params_gin ON simulation_snapshots USING GIN (parameters_snapshot);
