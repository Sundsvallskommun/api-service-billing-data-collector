CREATE TABLE scheduled_billing (
    id VARCHAR(36) NOT NULL,
    municipality_id VARCHAR(4) NOT NULL,
    external_id VARCHAR(64) NOT NULL,
    source VARCHAR(255) NOT NULL,
    billing_days_of_month VARCHAR(255) NOT NULL,
    billing_months VARCHAR(255) NOT NULL,
    last_billed DATETIME(6),
    next_scheduled_billing DATE,
    paused BOOLEAN NOT NULL DEFAULT FALSE,
    PRIMARY KEY (id),
    CONSTRAINT uq_external_id_municipality_source UNIQUE (external_id, municipality_id, source)
) ENGINE=InnoDB;

CREATE INDEX idx_municipality_id_external_id_source
    ON scheduled_billing (municipality_id, external_id, source);

CREATE INDEX idx_next_scheduled_billing_paused
    ON scheduled_billing (next_scheduled_billing, paused);
