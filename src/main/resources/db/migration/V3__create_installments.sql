-- Tabela de amortização linha a linha.
-- Persistida após o cálculo para evitar recomputação em exportações e histórico.
-- corrected_total é null quando inflation_correction não está habilitada.
CREATE TABLE installments (
    id                       UUID    PRIMARY KEY DEFAULT gen_random_uuid(),
    simulation_id            UUID    NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
    period_number            INTEGER NOT NULL,
    principal_balance_before NUMERIC NOT NULL,
    amortization             NUMERIC NOT NULL,
    interest                 NUMERIC NOT NULL,
    additional_charges       NUMERIC NOT NULL DEFAULT 0,
    total                    NUMERIC NOT NULL, -- amortization + interest + additional_charges
    principal_balance_after  NUMERIC NOT NULL,
    corrected_total          NUMERIC NULL,     -- null se correção monetária não habilitada

    CONSTRAINT uq_installment_period UNIQUE (simulation_id, period_number)
);

CREATE INDEX idx_installments_simulation_id ON installments (simulation_id);
