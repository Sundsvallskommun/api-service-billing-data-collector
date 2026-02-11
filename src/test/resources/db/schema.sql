
    create table counterpart_mapping (
        counterpart varchar(5) not null,
        legal_id_pattern varchar(12),
        stakeholder_type varchar(20),
        id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create table fallout (
        municipality_id varchar(4) not null,
        reported boolean default false,
        created datetime(6),
        modified datetime(6),
        request_id varchar(36),
        error_message varchar(1024),
        contract_id varchar(255),
        family_id varchar(255),
        flow_instance_id varchar(255),
        id varchar(255) not null,
        billing_record_wrapper text,
        opene_instance text,
        primary key (id)
    ) engine=InnoDB;

    create table history (
        municipality_id varchar(4) not null,
        created datetime(6),
        request_id varchar(36),
        contract_id varchar(255),
        family_id varchar(255),
        flow_instance_id varchar(255),
        id varchar(255) not null,
        location varchar(255),
        billing_record_wrapper text,
        primary key (id)
    ) engine=InnoDB;

    create table scheduled_billing (
        municipality_id varchar(4) not null,
        next_scheduled_billing date,
        paused bit not null,
        last_billed datetime(6),
        external_id varchar(64) not null,
        billing_days_of_month varchar(255) not null,
        billing_months varchar(255) not null,
        id varchar(255) not null,
        source enum ('CONTRACT','OPENE') not null,
        primary key (id)
    ) engine=InnoDB;

    create table scheduled_job_log (
        fetched_end_date date not null,
        fetched_start_date date not null,
        municipality_id varchar(4) not null,
        processed datetime(6) not null,
        id varchar(255) not null,
        primary key (id)
    ) engine=InnoDB;

    create index idx_stakeholder_type
       on counterpart_mapping (stakeholder_type);

    alter table if exists counterpart_mapping
       add constraint uq_legal_id_pattern unique (legal_id_pattern);

    create index idx_family_id
       on fallout (family_id);

    create index idx_flow_instance_id
       on fallout (flow_instance_id);

    create index idx_contract_id
       on fallout (contract_id);

    create index idx_municipality_id
       on fallout (municipality_id);

    create index idx_family_id
       on history (family_id);

    create index idx_flow_instance_id
       on history (flow_instance_id);

	create index idx_contract_id
       on history (contract_id);

    create index idx_municipality_id
       on history (municipality_id);

    create index idx_municipality_id_external_id_source
       on scheduled_billing (municipality_id, external_id, source);

    create index idx_next_scheduled_billing_paused 
       on scheduled_billing (next_scheduled_billing, paused);

    alter table if exists scheduled_billing 
       add constraint uq_external_id_municipality_source unique (external_id, municipality_id, source);

    create index idx_municipality_id 
       on scheduled_job_log (municipality_id);
