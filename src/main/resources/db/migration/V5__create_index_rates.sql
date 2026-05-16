-- Cache persistido dos índices econômicos consultados do BCB.
-- Evita chamadas repetidas à API externa e garante reprodutibilidade histórica.
-- A combinação (index_code, reference_date) deve ser única — um índice por período.
CREATE TABLE index_rates (
    id              UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    index_code      VARCHAR(10) NOT NULL, -- IPCA, CDI, SELIC, TR, IGP_M
    reference_date  DATE        NOT NULL,
    rate            NUMERIC     NOT NULL, -- valor do índice no período (decimal)
    source          VARCHAR(20) NOT NULL DEFAULT 'BCB_SGS', -- BCB_SGS, MANUAL
    fetched_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_index_rate_period UNIQUE (index_code, reference_date)
);

CREATE INDEX idx_index_rates_code_date ON index_rates (index_code, reference_date DESC);
