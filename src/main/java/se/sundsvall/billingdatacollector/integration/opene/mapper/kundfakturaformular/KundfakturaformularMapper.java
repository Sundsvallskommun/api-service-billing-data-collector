package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.extractValue;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.getString;

import org.springframework.stereotype.Component;

import se.sundsvall.billingdatacollector.integration.opene.OpenEMapper;
import se.sundsvall.billingdatacollector.model.dto.BillingRecordDto;

@Component
class KundfakturaformularMapper implements OpenEMapper {

    @Override
    public String getSupportedFamilyId() {
        return "358";
    }

    @Override
    public BillingRecordDto mapToBillingRecord(final byte[] xml) {
        if (getString(xml, "/FlowInstance/Values/BarakningarExtern1") == null) {
            return mapToInternalBillingRecord(xml);
        } else {
            return mapToExternalBillingRecord(xml);
        }
    }

    BillingRecordDto mapToInternalBillingRecord(final byte[] xml) {
        var result = extractValue(xml, InternFaktura.class);

        //System.err.println(result);

        throw new UnsupportedOperationException("INTERNAL INVOICE MAPPING NOT IMPLEMENTED");
    }

    BillingRecordDto mapToExternalBillingRecord(final byte[] xml) {
        var result = extractValue(xml, ExternFaktura.class);

        //System.err.println(result);

        // TODO: translate "personnummer" to "partyId" - but maybe not here ?

        throw new UnsupportedOperationException("EXTERNAL INVOICE MAPPING NOT IMPLEMENTED");
    }
}
