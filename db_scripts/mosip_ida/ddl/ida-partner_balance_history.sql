CREATE TABLE ida.partner_balance_history (
    transaction_id numeric,
    partner_id     character varying(128) NOT NULL,
    balance numeric NOT NULL,
	cr_by character varying(256) NOT NULL,
    cr_dtimes timestamp NOT NULL,
    upd_by character varying(256),
    upd_dtimes timestamp,
	CONSTRAINT  pk_partner_balance_history PRIMARY KEY (transaction_id)
);

COMMENT ON TABLE ida.partner_balance_history IS 'Partner Balance History: Table to Stores the every balance history information for each partner.';
COMMENT ON COLUMN ida.partner_balance_history.transaction_id IS 'Unique identifier of the payment PRN.';
COMMENT ON COLUMN ida.partner_balance_history.partner_id IS 'Unique identifier of the partner (Primary Key).';
COMMENT ON COLUMN ida.partner_balance_history.balance IS 'Current  balance available for the partner.';
COMMENT ON COLUMN ida.partner_balance_history.cr_by IS 'Created By : ID or name of the user who create / insert record.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.upd_by IS 'Updated By : ID or name of the user who update the record with new values.';
-- ddl-end --
COMMENT ON COLUMN ida.partner_balance_history.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';