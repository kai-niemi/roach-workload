create table if not exists event_${partition}
(
    id             uuid        not null default gen_random_uuid(),
    create_time    timestamptz not null default clock_timestamp(),
    aggregate_type string      not null,
    aggregate_id   string      not null,
    event_type     string      not null,
    payload        jsonb       null,

    primary key (id)
);