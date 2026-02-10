CREATE TABLE ida.auth_charges (
	type_code  character varying(128) NOT NULL,
	sub_type_code character varying(128) NOT NULL,
	amount numeric NOT NULL,
	effective_from timestamp NOT NULL,
	effective_to timestamp ,
	is_active boolean NOT NULL DEFAULT TRUE
	cr_by character varying(256) NOT NULL,
    cr_dtimes timestamp NOT NULL,
    upd_by character varying(256),
    upd_dtimes timestamp,
	CONSTRAINT  pk_idTypeCodeSubTypeCodeEffectiveFrom PRIMARY KEY (type_code,sub_type_code,effective_from)
);

COMMENT ON TABLE ida.auth_charges IS 'Auth Charges : Table to store  authentication charges by type and subtype with effective date versioning.';
COMMENT ON COLUMN ida.auth_charges.type_code IS 'Authentication type code identifying the charge category.';
COMMENT ON COLUMN ida.auth_charges.sub_type_code IS 'Authentication sub-type code for more granular charge classification.';
COMMENT ON COLUMN ida.auth_charges.amount IS 'Charge amount applicable for the given authentication type and subtype.';
COMMENT ON COLUMN ida.auth_charges.effective_from IS 'Start timestamp from which the charge amount is effective.';
COMMENT ON COLUMN ida.auth_charges.effective_to IS 'End timestamp until which the charge amount is effective (null means currently active).';
COMMENT ON COLUMN ida.auth_charges.is_active IS 'Indicates whether the authentication charge record is active (true) or inactive (false).';
COMMENT ON COLUMN ida.auth_charges.cr_by IS 'Created By : ID or name of the user who create / insert record.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_charges.cr_dtimes IS 'Created DateTimestamp : Date and Timestamp when the record is created/inserted.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_charges.upd_by IS 'Updated By : ID or name of the user who update the record with new values.';
-- ddl-end --
COMMENT ON COLUMN ida.auth_charges.upd_dtimes IS 'Updated DateTimestamp : Date and Timestamp when any of the fields in the record is updated with new values.';

