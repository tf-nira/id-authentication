CREATE TABLE ida.auth_sub_types (
	code character varying(128) NOT NULL,
	description character varying(128) NOT NULL,
	is_active boolean NOT NULL,
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
