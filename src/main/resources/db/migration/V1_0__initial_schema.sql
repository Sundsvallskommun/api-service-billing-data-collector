create table fallout
(
    billing_record_wrapper text,
    created                datetime(6),
    error_message          varchar(1024),
    family_id              varchar(255),
    flow_instance_id       varchar(255),
    id                     varchar(36) not null,
    modified               datetime(6),
    opene_instance         text,
    reported               boolean default false,
    request_id             varchar(36),
    primary key (id)
) engine = InnoDB;

create table history
(
    billing_record_wrapper text,
    created                date,
    family_id              varchar(255),
    flow_instance_id       varchar(255),
    id                     varchar(36) not null,
    location               varchar(255),
    request_id             varchar(36),
    primary key (id)
) engine = InnoDB;

create table scheduled_job_log
(
    fetched_end_date   date         not null,
    fetched_start_date date         not null,
    processed          datetime(6)  not null,
    id                 varchar(36) not null,
    primary key (id)
) engine = InnoDB;

create table shedlock
(
    lock_until timestamp(3) not null,
    locked_at  timestamp(3) not null default current_timestamp(3),
    locked_by  varchar(255) not null,
    name       varchar(64)  not null,
    primary key (name)
);

create index idx_family_id
    on fallout (family_id);

create index idx_flow_instance_id
    on fallout (flow_instance_id);

create index idx_family_id
    on history (family_id);

create index idx_flow_instance_id
    on history (flow_instance_id);
