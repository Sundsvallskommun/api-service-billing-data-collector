
create table fallout (
                         municipality_id varchar(4) not null,
                         reported boolean default false,
                         created datetime(6),
                         modified datetime(6),
                         request_id varchar(36),
                         error_message varchar(1024),
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
                         family_id varchar(255),
                         flow_instance_id varchar(255),
                         id varchar(255) not null,
                         location varchar(255),
                         billing_record_wrapper text,
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

create index idx_family_id
    on fallout (family_id);

create index idx_flow_instance_id
    on fallout (flow_instance_id);

create index idx_municipality_id
    on fallout (municipality_id);

create index idx_family_id
    on history (family_id);

create index idx_flow_instance_id
    on history (flow_instance_id);

create index idx_municipality_id
    on history (municipality_id);

create index idx_municipality_id
    on scheduled_job_log (municipality_id);
