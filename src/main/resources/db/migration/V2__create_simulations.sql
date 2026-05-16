-- Tabela central do sistema.
-- Armazena parâmetros e resultados consolidados de cada simulação.
-- Valores monetários sempre em NUMERIC para garantir precisão BigDecimal.
CREATE TABLE simulations (
    id                              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                         UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name                            VARCHAR(255) NOT NULL,

    -- Parâmetros do sistema de amortização
    amortization_system             VARCHAR(20)  NOT NULL, -- SAC, PRICE, AMERICAN, GERMAN, SAM
    principal                       NUMERIC      NOT NULL,
    interest_rate                   NUMERIC      NOT NULL, -- taxa por período (decimal, ex: 0.01 = 1%)
    rate_type                       VARCHAR(15)  NOT NULL, -- NOMINAL, EFFECTIVE
    term                            INTEGER      NOT NULL, -- número de períodos
    periodicity                     VARCHAR(15)  NOT NULL, -- MONTHLY, QUARTERLY, ANNUAL

    -- Opções adicionais
    cet_enabled                     BOOLEAN      NOT NULL DEFAULT FALSE,
    inflation_correction_enabled    BOOLEAN      NOT NULL DEFAULT FALSE,
    inflation_index                 VARCHAR(10)  NULL,     -- IPCA, IGP_M, CDI, null

    -- Resultados calculados (preenchidos após RunSimulationUseCase)
    total_paid                      NUMERIC      NULL,
    effective_cost_rate             NUMERIC      NULL,     -- CET, null se não habilitado

    -- Ciclo de vida
    status                          VARCHAR(15)  NOT NULL DEFAULT 'DRAFT', -- DRAFT, CALCULATED, EXPORTED

    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_simulations_user_id   ON simulations (user_id);
CREATE INDEX idx_simulations_status    ON simulations (status);
CREATE INDEX idx_simulations_created   ON simulations (created_at DESC);
