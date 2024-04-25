package se.sundsvall.billingdatacollector.integration.opene;

import static se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationConfiguration.CLIENT_ID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
    name = CLIENT_ID,
    url = "${integration.open-e.base-url}",
    configuration = OpenEIntegrationConfiguration.class
)
interface OpenEClient {

    String TEXT_XML_CHARSET_ISO_8859_1 = "text/xml; charset=ISO-8859-1";

    @GetMapping(path = "/api/instanceapi/getinstances/family/{familyId}", consumes = TEXT_XML_CHARSET_ISO_8859_1, produces = TEXT_XML_CHARSET_ISO_8859_1)
    byte[] getErrands(@PathVariable(name = "familyId") final String familyId,
        @RequestParam(name = "fromDate") final String fromDate,
        @RequestParam(name = "toDate") final String toDate);

    @GetMapping(path = "/api/instanceapi/getinstance/{flowInstanceId}/xml", consumes = TEXT_XML_CHARSET_ISO_8859_1, produces = TEXT_XML_CHARSET_ISO_8859_1)
    byte[] getErrand(@PathVariable(name = "flowInstanceId") final String flowInstanceId);
}
