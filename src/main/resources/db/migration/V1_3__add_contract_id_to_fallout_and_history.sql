alter table fallout add column contract_id varchar(255);

alter table history add column contract_id varchar(255);

create index idx_contract_id on fallout (contract_id);

create index idx_contract_id on history (contract_id);