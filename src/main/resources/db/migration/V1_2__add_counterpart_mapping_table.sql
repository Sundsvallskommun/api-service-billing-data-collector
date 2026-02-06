CREATE TABLE counterpart_mapping (
    id                 VARCHAR(36)  NOT NULL,
    legal_id           VARCHAR(255),
    legal_id_pattern   VARCHAR(255),
    stakeholder_type   VARCHAR(255),
    counterpart        VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB;

CREATE INDEX idx_legal_id ON counterpart_mapping (legal_id);
CREATE UNIQUE INDEX idx_legal_id_pattern ON counterpart_mapping (legal_id_pattern);
CREATE INDEX idx_stakeholder_type ON counterpart_mapping (stakeholder_type);
