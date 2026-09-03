-- Contingent-workforce VMS schema (PostgreSQL).
-- Idempotent so spring.sql.init can re-run it on each boot.

DROP TABLE IF EXISTS approvals;
DROP TABLE IF EXISTS timesheets;
DROP TABLE IF EXISTS workers;
DROP TABLE IF EXISTS vendors;
DROP TABLE IF EXISTS users;

CREATE TABLE vendors (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(200) NOT NULL,
    contact_email VARCHAR(200),
    logo_url      VARCHAR(500),
    status        VARCHAR(50)
);

CREATE TABLE workers (
    id        BIGSERIAL PRIMARY KEY,
    name      VARCHAR(200) NOT NULL,
    title     VARCHAR(200),
    vendor_id BIGINT REFERENCES vendors(id),
    status    VARCHAR(50),
    bill_rate DOUBLE PRECISION
);

CREATE TABLE timesheets (
    id          BIGSERIAL PRIMARY KEY,
    worker_id   BIGINT REFERENCES workers(id),
    week_ending VARCHAR(20),
    hours       DOUBLE PRECISION,
    status      VARCHAR(50)
);

CREATE TABLE approvals (
    id           BIGSERIAL PRIMARY KEY,
    timesheet_id BIGINT REFERENCES timesheets(id),
    approver     VARCHAR(200),
    decision     VARCHAR(50),
    comments     VARCHAR(500)
);

CREATE TABLE users (
    username     VARCHAR(100) PRIMARY KEY,
    password_md5 VARCHAR(64) NOT NULL,
    email        VARCHAR(200),
    role         VARCHAR(50)
);
