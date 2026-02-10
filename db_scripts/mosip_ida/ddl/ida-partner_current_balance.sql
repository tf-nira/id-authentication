CREATE TABLE ida.partner_current_balance (
	partner_id  character varying(128) NOT NULL,
	balance   numeric NOT NULL,
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

