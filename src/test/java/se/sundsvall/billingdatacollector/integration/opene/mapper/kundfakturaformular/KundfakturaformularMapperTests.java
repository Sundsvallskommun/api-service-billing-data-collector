package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

@ExtendWith(ResourceLoaderExtension.class)
class KundfakturaformularMapperTests {

    private final KundfakturaformularMapper mapper = new KundfakturaformularMapper();

    @Test
    void getSupportedFamilyId() {
        assertThat(mapper.getSupportedFamilyId()).isEqualTo("358");
    }

    @Test
    void mapToInternalBillingRecord(@Load("/open-e/flow-instance.internal.xml") final String xml) {
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> mapper.mapToBillingRecord(xml.getBytes(UTF_8)))
            .withMessage("INTERNAL INVOICE MAPPING NOT IMPLEMENTED");
    }

    @Test
    void mapToExternalBillingRecord(@Load("/open-e/flow-instance.external.xml") final String xml) {
        assertThatExceptionOfType(UnsupportedOperationException.class)
            .isThrownBy(() -> mapper.mapToBillingRecord(xml.getBytes(UTF_8)))
            .withMessage("EXTERNAL INVOICE MAPPING NOT IMPLEMENTED");
    }
}
