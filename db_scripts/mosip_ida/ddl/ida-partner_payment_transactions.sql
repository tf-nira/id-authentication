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


