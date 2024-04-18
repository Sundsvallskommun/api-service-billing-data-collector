package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import static se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorIntegrationConfiguration.CLIENT_ID;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;

@FeignClient(
    name = CLIENT_ID,
    url = "${integration.billing-preprocessor.base-url}",
    configuration = BillingPreprocessorIntegrationConfiguration.class
)
interface BillingPreprocessorClient {

    @PostMapping("/billingrecords")
    void createBillingRecord(@RequestBody BillingRecord billingRecord);

    @PostMapping("/billingrecords/batch")
    void createBillingRecords(@RequestBody List<BillingRecord> billingRecords);
}
