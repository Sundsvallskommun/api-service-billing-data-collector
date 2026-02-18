package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationProperties;
import se.sundsvall.billingdatacollector.integration.opene.OpenEMapper;
import se.sundsvall.billingdatacollector.integration.opene.util.ListUtil;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.getString;

@Component
class KundfakturaformularMapper implements OpenEMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(KundfakturaformularMapper.class);

	public static final String CATEGORY = "CUSTOMER_INVOICE";
	public static final String INVOICE_DESCRIPTION = "Kundfaktura";
	public static final String APPROVED_BY = "E_SERVICE";
	public static final int MAX_DESCRIPTION_LENGTH = 30;
	private static final String IS_EXTERNAL_INVOICE = "/FlowInstance/Values/BarakningarExtern1";

	private final OpenEIntegrationProperties properties;
	private final ListUtil listUtil;

	KundfakturaformularMapper(OpenEIntegrationProperties properties, ListUtil listUtil) {
		this.properties = properties;
		this.listUtil = listUtil;
	}

	@Override
	public String getSupportedFamilyId() {
		return properties.kundfakturaFormularFamilyId();
	}

	@Override
	public BillingRecordWrapper mapToBillingRecordWrapper(final byte[] xml) {
		LOGGER.info("Mapping xml to BillingRecordWrapper");
		var openeCollections = listUtil.parseLists(xml);

		// Check what kind of invoice it is and map accordingly
		if (getString(xml, IS_EXTERNAL_INVOICE) == null) {
			return InternalMapper.mapToInternalBillingRecord(xml, openeCollections);
		}

		return ExternalMapper.mapToExternalBillingRecord(xml, openeCollections);
	}
}
