-- Seed for legacy BillingSchedulerIT.test1_createBillingRecords. The test
-- triggers the contract scheduler against the 2026-00001 row.
INSERT INTO scheduled_billing (id, municipality_id, external_id, source, billing_days_of_month, billing_months, next_scheduled_billing, paused)
VALUES ('f0882f1d-06bc-47fd-b017-1d8307f5ce95', '2281', '2026-00001', 'CONTRACT', '1,15', '1,4,7,10', '2020-01-01', false);
