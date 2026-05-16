-- Encargos adicionais que compõem o CET (Custo Efetivo Total).
-- Registrados individualmente para auditoria e recálculo.
-- applies_on_period: null = incide em todas as parcelas; inteiro = período específico.
CREATE TABLE cet_charges (
    id                UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    simulation_id     UUID         NOT NULL REFERENCES simulations(id) ON DELETE CASCADE,
    description       VARCHAR(255) NOT NULL, -- "IOF", "Seguro MIP", "Tarifa de cadastro"
    charge_type       VARCHAR(15)  NOT NULL, -- FIXED, PERCENTAGE
    amount            NUMERIC      NOT NULL,
    applies_on_period INTEGER      NULL      -- null = todas as parcelas
);

CREATE INDEX idx_cet_charges_simulation_id ON cet_charges (simulation_id);
