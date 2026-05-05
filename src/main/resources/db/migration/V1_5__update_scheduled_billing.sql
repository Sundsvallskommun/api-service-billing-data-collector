ALTER TABLE scheduled_billing
    ADD COLUMN invoiced_in VARCHAR(32) NULL;

ALTER TABLE scheduled_billing
    DROP COLUMN final_billing_date;
