CREATE TABLE counterpart_mapping (
    id                 VARCHAR(36)  NOT NULL,
    legal_id_pattern   VARCHAR(12),
    stakeholder_type   VARCHAR(20),
    counterpart        VARCHAR(5) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_stakeholder_type ON counterpart_mapping (stakeholder_type);
ALTER TABLE if EXISTS counterpart_mapping
       ADD CONSTRAINT uq_legal_id_pattern UNIQUE (legal_id_pattern);
