INSERT INTO fallout (billing_record_wrapper, created, error_message, family_id,
                                          flow_instance_id, id, modified, opene_instance, reported,
                                          request_id, municipality_id)
VALUES ('{"billingRecord":{"id":null,"category":"KUNDFAKTURA","type":"EXTERNAL","status":"APPROVED","approvedBy":null,"approved":null,"recipient":{"partyId":"fb2f0290-3820-11ed-a261-0242ac120003","legalId":null,"organizationName":null,"firstName":"Kalle","lastName":"Anka","userId":null,"addressDetails":{"street":"ANKEBORG 222","careOf":null,"postalCode":"862 22","city":"ANKEBORG"}},"invoice":{"customerId":null,"description":null,"ourReference":null,"customerReference":null,"referenceId":"185376","date":null,"dueDate":null,"totalAmount":null,"invoiceRows":[{"descriptions":["Julmarknad Ankeborg. 3 marknadsplatser"],"detailedDescriptions":[],"totalAmount":2800.0,"vatCode":"00","costPerUnit":700.0,"quantity":4,"accountInformation":[{"costCenter":"43200000","subaccount":"345000","department":"315310","accuralKey":null,"activity":"4165","article":"3452000 - GULLGÅRDEN","project":null,"counterpart":"86000000"}]}]},"created":null,"modified":null},"familyId":"358","flowInstanceId":"185376","legalId":"199001012385"}',
        '2024-06-26 10:14:42.707561',
        'Bad Gateway: billing-preprocessor error: {status=502 Bad Gateway, title=Bad Gateway}',
        '358', '185376', 'b14f84dc-ec63-4c0d-b65c-070bfe57295e', '2024-06-26 10:14:57.576337', null,
        0, '83f2ba1f-8b9c-4df9-b05f-c27b40985eee', '2281');

INSERT INTO history (billing_record_wrapper, created, family_id,
                                          flow_instance_id, id, location, request_id, municipality_id)
VALUES ('{"billingRecord":{"id":null,"category":"KUNDFAKTURA","type":"EXTERNAL","status":"APPROVED","approvedBy":null,"approved":null,"recipient":{"partyId":"fb2f0290-3820-11ed-a261-0242ac120002","legalId":null,"organizationName":null,"firstName":"Kalle","lastName":"Anka","userId":null,"addressDetails":{"street":"ANKEBORG 150","careOf":null,"postalCode":"862 96","city":"ANKEBORG"}},"invoice":{"customerId":null,"description":null,"ourReference":null,"customerReference":null,"referenceId":"185375","date":null,"dueDate":null,"totalAmount":null,"invoiceRows":[{"descriptions":["Julmarknad Ankeborg. 3 marknadsplatser"],"detailedDescriptions":[],"totalAmount":2100.0,"vatCode":"00","costPerUnit":700.0,"quantity":3,"accountInformation":[{"costCenter":"43200000","subaccount":"345000","department":"315310","accuralKey":null,"activity":"4165","article":"3452000 - GULLGÅRDEN","project":null,"counterpart":"86000000"}]}]},"created":null,"modified":null},"familyId":"358","flowInstanceId":"185375","legalId":"199001012385"}',
        '2024-06-26', '358', '185375', 'b193283a-0f8b-491e-83d1-e5d806610f4b',
        '/billingrecords/9945f909-068e-4bce-b485-a4a563f8d0d7',
        '83f2ba1f-8b9c-4df9-b05f-c27b40985eee', '2281'),
       ('{"billingRecord":{"id":null,"category":"KUNDFAKTURA","type":"EXTERNAL","status":"APPROVED","approvedBy":null,"approved":null,"recipient":{"partyId":"fb2f0290-3820-11ed-a261-0242ac120002","legalId":null,"organizationName":null,"firstName":"Kalle","lastName":"Anka","userId":null,"addressDetails":{"street":"ANKEBORG 150","careOf":null,"postalCode":"862 96","city":"ANKEBORG"}},"invoice":{"customerId":null,"description":null,"ourReference":null,"customerReference":null,"referenceId":"185375","date":null,"dueDate":null,"totalAmount":null,"invoiceRows":[{"descriptions":["Julmarknad Ankeborg. 3 marknadsplatser"],"detailedDescriptions":[],"totalAmount":2100.0,"vatCode":"00","costPerUnit":700.0,"quantity":3,"accountInformation":[{"costCenter":"43200000","subaccount":"345000","department":"315310","accuralKey":null,"activity":"4165","article":"3452000 - GULLGÅRDEN","project":null,"counterpart":"86000000"}]}]},"created":null,"modified":null},"familyId":"358","flowInstanceId":"185375","legalId":"199001012385"}',
        '2024-06-26', '358', '185377', 'b193283a-0f8b-491e-83d1-e5d806610f4c',
        '/billingrecords/9945f909-068e-4bce-b485-a4a563f8d0d8',
        '83f2ba1f-8b9c-4df9-b05f-c27b40985eee', '2281');

INSERT INTO scheduled_job_log (fetched_end_date, fetched_start_date, processed, id, municipality_id)
VALUES ('2024-06-25', '2024-06-25', '2024-06-26 10:19:17.025118', 'ff298e4b-f14f-4494-8f3a-9da8b00207bb', '1984'),
       ('2024-06-24', '2024-06-24', '2024-06-26 10:19:17.025118', 'ff298e4b-f14f-4494-8f3a-9da8b00207bc', '2281'),
       ('2024-06-23', '2024-06-23', '2024-06-26 10:19:17.025118', 'ff298e4b-f14f-4494-8f3a-9da8b00207bd', '2281'),
       ('2024-06-22', '2024-06-22', '2024-06-26 10:19:17.025118', 'ff298e4b-f14f-4494-8f3a-9da8b00207be', '2281');
