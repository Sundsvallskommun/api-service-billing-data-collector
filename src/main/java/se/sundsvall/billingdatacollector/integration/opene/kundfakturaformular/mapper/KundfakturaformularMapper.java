package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.getString;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationProperties;
import se.sundsvall.billingdatacollector.integration.opene.OpenEMapper;
import se.sundsvall.billingdatacollector.integration.opene.util.ListUtil;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

@Component
class KundfakturaformularMapper implements OpenEMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(KundfakturaformularMapper.class);

	public static final String CATEGORY = "CUSTOMER_INVOICE";
	public static final String INVOICE_DESCRIPTION = "Kundfaktura";
	public static final String APPROVED_BY = "E_SERVICE";
	public static final int MAX_DESCRIPTION_LENGTH = 30;

	private final ExternalMapper externalMapper;
	private final InternalMapper internalMapper;
	private final OpenEIntegrationProperties properties;
	private final ListUtil listUtil;

	KundfakturaformularMapper(ExternalMapper externalMapper, InternalMapper internalMapper, OpenEIntegrationProperties properties, ListUtil listUtil) {
		this.externalMapper = externalMapper;
		this.internalMapper = internalMapper;
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
		BillingRecordWrapper wrapper;

		if (getString(xml, "/FlowInstance/Values/BarakningarExtern1") == null) {
			wrapper = internalMapper.mapToInternalBillingRecord(xml, openeCollections);
		} else {
			wrapper = externalMapper.mapToExternalBillingRecord(xml, openeCollections);
		}

		return wrapper;
	}
}
