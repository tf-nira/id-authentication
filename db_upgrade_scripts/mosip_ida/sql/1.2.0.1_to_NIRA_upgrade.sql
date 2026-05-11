\c mosip_ida

REASSIGN OWNED BY sysadmin TO postgres;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA ida FROM idauser;

REVOKE ALL PRIVILEGES ON ALL TABLES IN SCHEMA ida FROM sysadmin;

GRANT SELECT, INSERT, TRUNCATE, REFERENCES, UPDATE, DELETE ON ALL TABLES IN SCHEMA ida TO idauser;

GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA ida TO postgres;

----------------------------------------------Multiple table level changes on IDA db-------------------------------------------------------
ALTER TABLE ida.partner_data ADD COLUMN requires_payment boolean NOT NULL DEFAULT false;
--------------------------------------------------------------------------------------------------
ALTER TABLE ida.partner_data ADD COLUMN IF NOT EXISTS partner_auth_type character varying(128), ADD COLUMN IF NOT EXISTS partner_group character varying(128);
--------------------------------------------------------------------------------------------------
ALTER TABLE ida.auth_transaction ADD COLUMN amount numeric;
--------------------------------------------------------------------------------------------------
CREATE TABLE ida.auth_types (
	code character varying(128) NOT NULL,
	description character varying(128) NOT NULL,
	is_active boolean NOT NULL DEFAULT TRUE,
	cr_by character varying(256) NOT NULL,
    cr_dtimes timestamp NOT NULL,
    upd_by character varying(256),
    upd_dtimes timestamp,
	CONSTRAINT  pk_idCode PRIMARY KEY (code)
);

COMMENT ON TABLE ida.auth_types IS 'Auth Types : Table to store authentication types used in the system.';
COMMENT ON COLUMN ida.auth_types.code IS 'Unique code representing the authentication type.';
COMMENT ON COLUMN ida.auth_types.description IS 'Description of the authentication type.';
COMMENT ON COLUMN ida.auth_types.is_active IS 'Indicates whether the authentication type is active (true) or inactive (false).';
COMMENT ON COLUMN ida.auth_types.cr_by IS 'Created By : ID or name of the user who create / insert record.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_types.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_types.upd_by IS 'Updated By : ID or name of the user who update the record with new values.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_types.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';
---------------------------------------------------------------------------------------------------------------

CREATE TABLE ida.auth_sub_types (
	code character varying(128) NOT NULL,
	description character varying(128) NOT NULL,
	is_active boolean NOT NULL DEFAULT TRUE,
	cr_by character varying(256) NOT NULL,
    cr_dtimes timestamp NOT NULL,
    upd_by character varying(256),
    upd_dtimes timestamp,
	CONSTRAINT  pk_idSubTypeCode PRIMARY KEY (code)
);

COMMENT ON TABLE ida.auth_sub_types IS 'Auth Sub Types : Table to store authentication sub types used in the system.';
COMMENT ON COLUMN ida.auth_sub_types.code IS 'Unique code representing the authentication sub type.';
COMMENT ON COLUMN ida.auth_sub_types.description IS 'Description of the authentication sub type.';
COMMENT ON COLUMN ida.auth_sub_types.is_active IS 'Indicates whether the authentication sub type is active (true) or inactive (false).';
COMMENT ON COLUMN ida.auth_sub_types.cr_by IS 'Created By : ID or name of the user who create / insert record.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_sub_types.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_sub_types.upd_by IS 'Updated By : ID or name of the user who update the record with new values.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_sub_types.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';
-------------------------------------------------------------------------------------------------------------------------


CREATE TABLE ida.auth_charges (
	type_code character varying(128) NOT NULL,
	sub_type_code character varying(128) NOT NULL,
	amount numeric NOT NULL,
	effective_from timestamp NOT NULL,
	effective_to timestamp,
	is_active boolean NOT NULL,
	cr_by character varying(256) NOT NULL,
    cr_dtimes timestamp NOT NULL,
    upd_by character varying(256),
    upd_dtimes timestamp,
	CONSTRAINT  pk_idTypeCodeSubTypeCodeEffectiveFrom PRIMARY KEY (type_code,sub_type_code,effective_from),
	CONSTRAINT fk_auth_charges_type FOREIGN KEY (type_code) REFERENCES ida.auth_types(code),
	CONSTRAINT fk_auth_charges_sub_type FOREIGN KEY (sub_type_code) REFERENCES ida.auth_sub_types(code)
);

COMMENT ON TABLE ida.auth_charges IS 'Auth Charges : Table to store  authentication charges by type and subtype with effective date versioning.';
COMMENT ON COLUMN ida.auth_charges.type_code IS 'Authentication type code identifying the charge category.';
COMMENT ON COLUMN ida.auth_charges.sub_type_code IS 'Authentication sub-type code for more granular charge classification.';
COMMENT ON COLUMN ida.auth_charges.amount IS 'Charge amount applicable for the given authentication type and subtype.';
COMMENT ON COLUMN ida.auth_charges.effective_from IS 'Start timestamp from which the charge amount is effective.';
COMMENT ON COLUMN ida.auth_charges.effective_to IS 'End timestamp until which the charge amount is effective (null means currently active).';
COMMENT ON COLUMN ida.auth_charges.is_active IS 'Indicates whether the authentication type is active (true) or inactive (false).';
COMMENT ON COLUMN ida.auth_charges.cr_by IS 'Created By : ID or name of the user who create / insert record.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_charges.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_charges.upd_by IS 'Updated By : ID or name of the user who update the record with new values.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_charges.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';

-------------------------------------------------------------------------------------------------------------------

CREATE TABLE ida.partner_current_balance (
	partner_id character varying(128) NOT NULL,
	balance numeric NOT NULL,
	cr_by character varying(256) NOT NULL,
    cr_dtimes timestamp NOT NULL,
    upd_by character varying(256),
    upd_dtimes timestamp,
	CONSTRAINT  pk_idPartner PRIMARY KEY (partner_id)
);

COMMENT ON TABLE ida.partner_current_balance IS 'Partner Current Balance : Table to Stores the current balance information for each partner.';
COMMENT ON COLUMN ida.partner_current_balance.partner_id IS 'Unique identifier of the partner (Primary Key).';
COMMENT ON COLUMN ida.partner_current_balance.balance IS 'Current  balance available for the partner.';
COMMENT ON COLUMN ida.partner_current_balance.cr_by IS 'Created By : ID or name of the user who create / insert record.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_current_balance.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_current_balance.upd_by IS 'Updated By : ID or name of the user who update the record with new values.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_current_balance.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';

------------------------------------------------------------------------------------------------------------------

CREATE TABLE ida.partner_payment_transactions (
	transaction_id character varying(128) NOT NULL,
	amount numeric NOT NULL,
	partner_id character varying(128) NOT NULL,
    log_dtimes timestamp NOT NULL,
	is_processed boolean NOT NULL DEFAULT false, 
	CONSTRAINT  pk_idTransactionId PRIMARY KEY (transaction_id)
);

COMMENT ON TABLE ida.partner_payment_transactions IS 'Partner Payment Transactions : Table to Stores payment transaction records associated with partners.';
COMMENT ON COLUMN ida.partner_payment_transactions.partner_id IS 'Unique identifier of the partner associated with the payment transaction.';
COMMENT ON COLUMN ida.partner_payment_transactions.amount IS 'Transaction amount for the partner payment.';
COMMENT ON COLUMN ida.partner_payment_transactions.transaction_id IS 'Unique identifier of the payment transaction.';
COMMENT ON COLUMN ida.partner_payment_transactions.log_dtimes IS 'Timestamp when the payment transaction was logged in the system.';
COMMENT ON COLUMN ida.partner_payment_transactions.is_processed IS 'Flag to track if the payment transaction has been processed for idempotency.';



-------------------------------------------------------------------------------------------------------------------------

CREATE TABLE ida.partner_balance_history (
    transaction_id character varying(128) NOT NULL,
    partner_id     character varying(128) NOT NULL,
    balance numeric NOT NULL,
	cr_by character varying(256) NOT NULL,
    cr_dtimes timestamp NOT NULL,
    upd_by character varying(256),
    upd_dtimes timestamp,
	CONSTRAINT  pk_partner_balance_history PRIMARY KEY (transaction_id)
);

COMMENT ON TABLE ida.partner_balance_history IS 'Partner Balance History: Table to Stores the every balance history information for each partner.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.transaction_id IS 'Unique identifier of the payment PRN.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.partner_id IS 'Unique identifier of the partner (Primary Key).';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.balance IS 'Current  balance available for the partner.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.cr_by IS 'Created By : ID or name of the user who create / insert record.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.upd_by IS 'Updated By : ID or name of the user who update the record with new values.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';
-- ddl-end --

--------------------------------------------------------------------------------------------------------------------------

INSERT INTO ida.auth_types(code, description, is_active, cr_by, cr_dtimes, upd_by, upd_dtimes)
VALUES
('otp', 'One Time Password Authentication', true, 'SYSTEM', NOW(), 'SYSTEM', NOW()),
('demo', 'Demographic Authentication', true, 'SYSTEM', NOW(), 'SYSTEM', NOW()),
('bio', 'Biometric Authentication', true, 'SYSTEM', NOW(), 'SYSTEM', NOW());

INSERT INTO ida.auth_sub_types(code, description, is_active, cr_by, cr_dtimes, upd_by, upd_dtimes)
VALUES ('auth', 'auth', true, 'SYSTEM', NOW(), 'SYSTEM', NOW()),
('ekyc', 'ekyc', true, 'SYSTEM', NOW(), 'SYSTEM', NOW());
 
INSERT INTO ida.auth_charges (type_code, sub_type_code, amount, effective_from, effective_to, is_active, cr_by, cr_dtimes, upd_by, upd_dtimes)
VALUES('otp', 'auth', 100.00, NOW(), NULL, true, 'SYSTEM', NOW(), 'SYSTEM', NOW()),
('demo', 'auth', 100.00, NOW(), NULL, true, 'SYSTEM', NOW(), 'SYSTEM', NOW()),
('bio', 'auth', 100.00, NOW(), NULL, true, 'SYSTEM', NOW(), 'SYSTEM', NOW()),
('otp', 'ekyc', 500.00, NOW(), NULL, true, 'SYSTEM', NOW(), 'SYSTEM', NOW()),
('demo', 'ekyc', 500.00, NOW(), NULL, true, 'SYSTEM', NOW(), 'SYSTEM', NOW()),
('bio', 'ekyc', 500.00, NOW(), NULL, true, 'SYSTEM', NOW(), 'SYSTEM', NOW());
