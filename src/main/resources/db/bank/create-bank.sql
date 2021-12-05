create table if not exists account
(
    id             uuid           not null default gen_random_uuid(),
    region         string         not null default crdb_internal.locality_value('region'),
    balance        decimal(19, 2) not null,
    currency       string(3)      not null,
    name           string(128)    not null,
    description    string(256)    null,
    type           string(1)      not null,
    closed         boolean        not null default false,
    allow_negative integer        not null default 0,
    updated        timestamptz    not null default clock_timestamp(),

    family         update_often(balance, updated),
    family         update_never(currency, name, description, type, closed, allow_negative),

    primary key (region, id)
);

create table if not exists transaction
(
    id               uuid      not null default gen_random_uuid(),
    region           string    not null default crdb_internal.locality_value('region'),
    booking_date     date      not null default current_date(),
    transfer_date    date      not null default current_date(),
    transaction_type string(3) not null,

    primary key (region, id)
);

create table if not exists transaction_item
(
    transaction_id     uuid           not null,
    transaction_region string         not null,
    account_id         uuid           not null,
    account_region     string         not null,
    amount             decimal(19, 2) not null,
    currency           string(3)      not null,
    note               string,
    running_balance    decimal(19, 2) not null,

    primary key (transaction_region, transaction_id, account_region, account_id)
);

------------------------------------------------
-- Constraints on account
------------------------------------------------

alter table if exists account
    add constraint check_account_type check (type in ('A', 'L', 'E', 'R', 'C'));
alter table if exists account
    add constraint check_account_allow_negative check (allow_negative between 0 and 1);
alter table if exists account
    add constraint check_account_positive_balance check (balance * abs(allow_negative - 1) >= 0);

------------------------------------------------
-- Constraints on transaction_item
------------------------------------------------

-- alter table transaction_item
--     add constraint fk_region_ref_transaction
--         foreign key (transaction_region, transaction_id) references transaction (region, id);

-- alter table transaction_item
--     add constraint fk_region_ref_account
--         foreign key (account_region, account_id) references account (region, id);
