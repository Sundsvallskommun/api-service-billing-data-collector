package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNDSVALLS_MUNICIPALITY;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.extractValue;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.getString;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationProperties;
import se.sundsvall.billingdatacollector.integration.opene.OpenEMapper;
import se.sundsvall.billingdatacollector.integration.opene.mapper.MapperHelper;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.ExternFaktura;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.InternFaktura;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.OpeneCollections;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.AnsvarExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.BarakningarExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.ObjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.UnderkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.external.VerksamhetExtern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.AktivitetskontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.AnsvarIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.BerakningIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.UnderkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular.model.internal.VerksamhetIntern;
import se.sundsvall.billingdatacollector.integration.opene.util.ListUtil;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import generated.se.sundsvall.billingpreprocessor.AccountInformation;
import generated.se.sundsvall.billingpreprocessor.AddressDetails;
import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.InvoiceRow;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.billingpreprocessor.Status;
import generated.se.sundsvall.billingpreprocessor.Type;

@Component
class KundfakturaformularMapper implements OpenEMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(KundfakturaformularMapper.class);

	private static final String CATEGORY = "CUSTOMER_INVOICE";
	private static final String INVOICE_DESCRIPTION = "Kundfaktura";
	private static final String APPROVED_BY = "E_SERVICE";
	private static final int MAX_DESCRIPTION_LENGTH = 30;

	private final MapperHelper mapperHelper;
	private final OpenEIntegrationProperties properties;
	private final ListUtil listUtil;

	KundfakturaformularMapper(MapperHelper mapperHelper, OpenEIntegrationProperties properties, ListUtil listUtil) {
		this.mapperHelper = mapperHelper;
		this.properties = properties;
		this.listUtil = listUtil;
	}

	@Override
	public String getSupportedFamilyId() {
		return properties.kundfakturaFormularFamilyId();
	}

	@Override
	public BillingRecordWrapper mapToBillingRecordWrapper(final byte[] xml) {
		var openeCollections = listUtil.parseLists(xml);
		BillingRecordWrapper wrapper;

		if (getString(xml, "/FlowInstance/Values/BarakningarExtern1") == null) {
			wrapper = mapToInternalBillingRecord(xml, openeCollections);
		} else {
			wrapper = mapToExternalBillingRecord(xml, openeCollections);
		}

		sanityCheck(wrapper);
		return wrapper;
	}

	// Perform a sanity check on the billing record
	// Basically check if we have any invoice rows to send to the preprocessor
	private void sanityCheck(BillingRecordWrapper wrapper) {
		// Check if we have something to send to the preprocessor
		if (wrapper.getBillingRecord().getInvoice().getInvoiceRows().isEmpty()) {
			LOGGER.warn("No invoice rows found for flowInstanceId: {}", wrapper.getFlowInstanceId());
			throw Problem.builder()
				.withTitle("No invoice rows found for flowInstanceId: " + wrapper.getFlowInstanceId())
				.withStatus(org.zalando.problem.Status.INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	BillingRecordWrapper mapToInternalBillingRecord(final byte[] xml, OpeneCollections collections) {
		LOGGER.info("Mapping to internal billing record");
		var result = extractValue(xml, InternFaktura.class);

		// Get the customerId, we will reuse this and add a "1" in front of it to create the counterpart
		var customerId = mapperHelper.getLeadingDigitsFromString(result.forvaltningSomSkaBetala());

		var billingRecord = new BillingRecord()
			.category(CATEGORY)
			.status(Status.APPROVED)
			.approvedBy(APPROVED_BY)
			.type(Type.INTERNAL)
			.recipient(new Recipient()
				.organizationName(SUNDSVALLS_MUNICIPALITY)
				.legalId(SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER))
			.invoice(createInternalInvoice(result, collections, customerId));

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withFlowInstanceId(result.flowInstanceId())
			.withFamilyId(result.familyId())
			.build();
	}

	Invoice createInternalInvoice(InternFaktura internFaktura, OpeneCollections collections, String customerId) {
		return new Invoice()
			.customerId(customerId)
			.customerReference(getTruncatedInternalReference(internFaktura.internReferens()))
			.description(truncateString(INVOICE_DESCRIPTION, MAX_DESCRIPTION_LENGTH))  //Cannot be more than 30 chars
			.ourReference(getInternalSellerName(internFaktura))
			.referenceId(internFaktura.flowInstanceId())
			.invoiceRows(createInternalInvoiceRows(collections, customerId));
	}

	List<InvoiceRow> createInternalInvoiceRows(OpeneCollections collections, String customerId) {
		List<InvoiceRow> invoiceRows = new ArrayList<>();

		for(int index = 1; index < collections.getNumberOfRows() + 1; index++ ) {
			LOGGER.info("Creating internal invoice row for index: {}", index);

			var invoiceRow = new InvoiceRow()

				.descriptions(ofNullable(truncateString(
					ofNullable(collections.getBerakningInternMap().get(index)).map(BerakningIntern::getFakturatextIntern).orElse(null), MAX_DESCRIPTION_LENGTH))
					.map(List::of)
					.orElseGet(Collections::emptyList))
				.quantity(mapperHelper.convertStringToFloat(
					ofNullable(collections.getBerakningInternMap().get(index)).map(BerakningIntern::getAntalIntern).orElse(null)))
				.costPerUnit(mapperHelper.convertStringToFloat(
					ofNullable(collections.getBerakningInternMap().get(index)).map(BerakningIntern::getAPrisIntern).orElse(null)))
				.accountInformation(new AccountInformation()
					.costCenter(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAnsvarInternMap().get(index)).map(AnsvarIntern::getValue).orElse(null)))
					.subaccount(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getUnderkontoInternMap().get(index)).map(UnderkontoIntern::getValue).orElse(null)))
					.department(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getVerksamhetInternMap().get(index)).map(VerksamhetIntern::getValue).orElse(null)))
					.activity(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAktivitetskontoInternMap().get(index)).map(AktivitetskontoIntern::getValue).orElse(null)))
					.counterpart(mapperHelper.getInternalMotpartNumbers(customerId)));

			invoiceRows.add(invoiceRow);
		}

		return invoiceRows;
	}

	BillingRecordWrapper mapToExternalBillingRecord(final byte[] xml, OpeneCollections collections) {
		LOGGER.info("Mapping to external billing record");
		var result = extractValue(xml, ExternFaktura.class);

		var billingRecord = new BillingRecord()
			.category(CATEGORY)
			.status(Status.APPROVED)
			.approvedBy(APPROVED_BY)
			.type(Type.EXTERNAL)
			.recipient(new Recipient()
				.firstName(result.fornamn())
				.lastName(result.efternamn())
				.addressDetails(new AddressDetails()
					.street(result.adress())
					.postalCode(result.postnummer())
					.city(result.ort())))
			.invoice(createExternalInvoice(result, collections, result.personnummer()));

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(result.personnummer())
			.withFamilyId(result.familyId())
			.withFlowInstanceId(result.flowInstanceId())
			.build();
	}

	Invoice createExternalInvoice(ExternFaktura externFaktura, OpeneCollections collections, String personalNumber) {
		return new Invoice()
			.customerReference(getExternalCustomerReference(externFaktura))
			.customerId(externFaktura.personnummer())
			.description(truncateString(INVOICE_DESCRIPTION, MAX_DESCRIPTION_LENGTH))  //Cannot be more than 30 chars
			.ourReference(getExternalSellerName(externFaktura))
			.referenceId(externFaktura.flowInstanceId())
			.invoiceRows(createExternalInvoiceRows(collections, personalNumber));
	}

	List<InvoiceRow> createExternalInvoiceRows(OpeneCollections collections, String customerId) {
		List<InvoiceRow> invoiceRows = new ArrayList<>();

		for(int index = 1; index < collections.getNumberOfRows() + 1; index++ ) {
			var invoiceRow = new InvoiceRow()
				.descriptions(ofNullable(truncateString(
					ofNullable(collections.getBarakningarExternMap().get(index)).map(BarakningarExtern::getFakturatextExtern).orElse(null), MAX_DESCRIPTION_LENGTH))
					.map(List::of)
					.orElseGet(Collections::emptyList))
				.quantity(mapperHelper.convertStringToFloat(
					ofNullable(collections.getBarakningarExternMap().get(index)).map(BarakningarExtern::getAntalExtern).orElse(null)))
				.costPerUnit(mapperHelper.convertStringToFloat(
					ofNullable(collections.getBarakningarExternMap().get(index)).map(BarakningarExtern::getAPrisExtern).orElse(null)))
				.vatCode(collections.getMomssatsExternMap().get(index).getValue())
				.accountInformation(new AccountInformation()
					.activity(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAktivitetskontoInternMap().get(index)).map(AktivitetskontoIntern::getValue).orElse(null)))
					.article(ofNullable(collections.getObjektKontoExternMap().get(index)).map(ObjektkontoExtern::getValue).orElse(null))
					.costCenter(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAnsvarExternMap().get(index)).map(AnsvarExtern::getValue).orElse(null)))
					.counterpart(mapperHelper.getInternalMotpartNumbers(customerId))
					.department(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getVerksamhetExternMap().get(index)).map(VerksamhetExtern::getValue).orElse(null)))
					.subaccount(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getUnderkontoExternMap().get(index)).map(UnderkontoExtern::getValue).orElse(null))));
			invoiceRows.add(invoiceRow);
		}

		return invoiceRows;
	}

	String getInternalSellerName(InternFaktura internFaktura) {
		return internFaktura.saljarensFornamn() + " " + internFaktura.saljarensEfternamn();
	}

	String getExternalSellerName(ExternFaktura externFaktura) {
		return externFaktura.saljarensFornamn() + " " + externFaktura.saljarensEfternamn();
	}

	String getExternalCustomerReference(ExternFaktura externFaktura) {
		return externFaktura.kontaktuppgifterPrivatpersonFornamn() + " " + externFaktura.kontaktuppgifterPrivatpersonEfternamn();
	}

	/**
	 * Get the internal reference and only the first "word".
	 * e.g. 1NAM16NAM - Name Namesson will return 1NAM16NAM
	 * @param reference The internal reference
	 * @return The internal reference if present, otherwise null
	 */
	String getTruncatedInternalReference(String reference) {
		return ofNullable(reference)
			.map(ref -> ref.split(" ")[0])
			.orElse(null);
	}

	/**
	 * Limit the length of a string, if it's more than the maxLength it will be truncated.
	 * @param string the string to potentially limit
	 * @param maxLength the maximum length of the string
	 * @return the string if it's less than maxLength, otherwise the string truncated to maxLength
	 */
	String truncateString(String string, int maxLength) {
		if(isNotBlank(string) && string.length() >= maxLength) {
			return string.substring(0, maxLength);
		}
		return string;
	}
}
