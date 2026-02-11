\c mosip_ida

REASSIGN OWNED BY sysadmin TO postgres;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA ida FROM idauser;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA ida FROM sysadmin;

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE ON ALL TABLES IN SCHEMA ida TO idauser;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ida TO postgres;
---------------------------------------------------------------------------------------------

ALTER TABLE ida.partner_data DROP COLUMN requires_payment;
ALTER TABLE ida.auth_transaction DROP COLUMN amount;

DROP TABLE IF EXISTS ida.auth_charges;
DROP TABLE IF EXISTS ida.auth_types;
DROP TABLE IF EXISTS ida.auth_sub_types;
DROP TABLE IF EXISTS ida.partner_current_balance;
DROP TABLE IF EXISTS ida.partner_payment_transactions;