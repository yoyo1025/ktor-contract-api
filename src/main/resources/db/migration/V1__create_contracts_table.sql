CREATE TABLE contracts (
    id            UUID PRIMARY KEY,
    title         VARCHAR(255) NOT NULL,
    counterparty  VARCHAR(255) NOT NULL,
    start_date    DATE NOT NULL,
    end_date      DATE,
    auto_renewal  BOOLEAN NOT NULL DEFAULT FALSE,
    status        VARCHAR(20) NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_contracts_counterparty ON contracts(counterparty);
CREATE INDEX idx_contracts_status ON contracts(status);
CREATE INDEX idx_contracts_end_date ON contracts(end_date);
