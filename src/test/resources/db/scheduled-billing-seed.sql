-- Seed of three scheduled_billing rows used by the CRUD-style tests in
-- ScheduledBillingIT (integration) and ScheduledBillingRepositoryTest (JPA
-- slice). Kept out of testdata.sql so the rest of the test suite isn't
-- coupled to specific scheduled_billing fixtures.
INSERT INTO scheduled_billing (id, municipality_id, external_id, source, billing_days_of_month, billing_months, next_scheduled_billing, paused)
VALUES ('f0882f1d-06bc-47fd-b017-1d8307f5ce95', '2281', '2026-00001', 'CONTRACT', '1,15', '1,4,7,10', '2020-01-01', false),
       ('a1b2c3d4-e5f6-7890-abcd-ef1234567890', '2281', 'external-id-for-get-test', 'CONTRACT', '10,20', '3,6,9,12', '2025-03-10', false),
       ('d3e4f5a6-b7c8-9012-def0-123456789abc', '2281', 'external-id-for-delete', 'OPENE', '5', '1,7', '2025-01-05', true);
