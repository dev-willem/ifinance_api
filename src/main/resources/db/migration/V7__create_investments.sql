CREATE TABLE investments (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES users(id),
    name VARCHAR(255),
    investment_type VARCHAR(50) NOT NULL,
    rate_basis VARCHAR(50) NOT NULL,
    rate_value NUMERIC(20, 10) NOT NULL,
    principal NUMERIC(20, 2) NOT NULL,
    term_days INT NOT NULL,
    start_date DATE NOT NULL,
    is_tax_exempt BOOLEAN NOT NULL DEFAULT FALSE,
    gross_return NUMERIC(20, 2),
    net_return NUMERIC(20, 2),
    ir_rate NUMERIC(20, 10),
    ir_amount NUMERIC(20, 2),
    gross_annual_rate NUMERIC(20, 10),
    net_annual_rate NUMERIC(20, 10),
    index_rate_used NUMERIC(20, 10),
    status VARCHAR(50) NOT NULL DEFAULT 'CALCULATED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_investments_user_id ON investments(user_id);
CREATE INDEX idx_investments_created_at ON investments(created_at DESC);
