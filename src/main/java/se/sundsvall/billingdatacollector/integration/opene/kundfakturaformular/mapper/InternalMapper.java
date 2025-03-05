package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import static java.util.Optional.ofNullable;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.APPROVED_BY;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.CATEGORY;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.INVOICE_DESCRIPTION;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.MAX_DESCRIPTION_LENGTH;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.BillingRecordConstants.SUNDSVALLS_MUNICIPALITY;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.BillingRecordConstants.SUNDSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.extractValue;

import generated.se.sundsvall.billingpreprocessor.AccountInformation;
import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.InvoiceRow;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.billingpreprocessor.Status;
import generated.se.sundsvall.billingpreprocessor.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.InternFaktura;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.OpeneCollections;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.AktivitetskontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.AnsvarIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.BerakningIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.SummeringIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.UnderkontoIntern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.internal.VerksamhetIntern;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

final class InternalMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(InternalMapper.class);

	private InternalMapper() {
		// Not meant to be instantiated
	}

	static BillingRecordWrapper mapToInternalBillingRecord(final byte[] xml, OpeneCollections collections) {
		LOGGER.info("Mapping to internal billing record");
		var result = extractValue(xml, InternFaktura.class);

		// Get the customerId, we will reuse this and add a "1" in front of it to create the counterpart
		var customerId = MapperHelper.getLeadingDigitsFromString(result.payingAdministration());

		var billingRecord = new BillingRecord()
			.category(CATEGORY)
			.status(Status.APPROVED)
			.approvedBy(APPROVED_BY)
			.type(Type.INTERNAL)
			.recipient(new Recipient()
				.organizationName(SUNDSVALLS_MUNICIPALITY)
				.legalId(SUNDSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER))
			.invoice(createInternalInvoice(result, collections, customerId));

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withFlowInstanceId(result.flowInstanceId())
			.withFamilyId(result.familyId())
			.withLegalId(SUNDSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER)
			.build();
	}

	static Invoice createInternalInvoice(InternFaktura internFaktura, OpeneCollections collections, String customerId) {
		return new Invoice()
			.customerId(customerId)
			.customerReference(getTruncatedInternalReference(internFaktura.referenceSundsvallsMunicipality()))
			.description(MapperHelper.truncateString(INVOICE_DESCRIPTION, MAX_DESCRIPTION_LENGTH))  // Cannot be more than 30 chars
			.ourReference(getInternalSellerName(internFaktura))
			.invoiceRows(createInternalInvoiceRows(collections, customerId));
	}

	static List<InvoiceRow> createInternalInvoiceRows(OpeneCollections collections, String customerId) {
		List<InvoiceRow> invoiceRows = new ArrayList<>();

		for (int index = 1; index < collections.getNumberOfRows() + 1; index++) {
			LOGGER.info("Creating internal invoice row for index: {}", index);

			var invoiceRow = new InvoiceRow()
				.descriptions(ofNullable(MapperHelper.truncateString(
					ofNullable(collections.getBerakningInternMap().get(index)).map(BerakningIntern::getFakturatextIntern).orElse(null), MAX_DESCRIPTION_LENGTH))
					.map(List::of)
					.orElseGet(Collections::emptyList))
				.quantity(MapperHelper.convertStringToBigDecimal(
					ofNullable(collections.getBerakningInternMap().get(index)).map(BerakningIntern::getAntalIntern).orElse(null)))
				.costPerUnit(MapperHelper.convertStringToBigDecimal(
					ofNullable(collections.getBerakningInternMap().get(index)).map(BerakningIntern::getAPrisIntern).orElse(null)))
				.accountInformation(List.of(new AccountInformation()
					.costCenter(MapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAnsvarInternMap().get(index)).map(AnsvarIntern::getValue).orElse(null)))
					.subaccount(MapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getUnderkontoInternMap().get(index)).map(UnderkontoIntern::getValue).orElse(null)))
					.department(MapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getVerksamhetInternMap().get(index)).map(VerksamhetIntern::getValue).orElse(null)))
					.activity(MapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAktivitetskontoInternMap().get(index)).map(AktivitetskontoIntern::getValue).orElse(null)))
					.counterpart(MapperHelper.getInternalCounterPartNumbers(customerId))
					.amount(MapperHelper.convertStringToBigDecimal(
						ofNullable(collections.getSummeringInternMap().get(index)).map(SummeringIntern::getTotSummeringIntern).orElse(null)))));

			invoiceRows.add(invoiceRow);
		}

		return invoiceRows;
	}

	/**
	 * Get the internal reference and only the first "word".
	 * e.g. 1NAM16NAM - Name Namesson will return 1NAM16NAM
	 * 
	 * @param  reference The internal reference
	 * @return           The internal reference if present, otherwise null
	 */
	static String getTruncatedInternalReference(String reference) {
		return ofNullable(reference)
			.map(ref -> ref.split(" ")[0])
			.orElse(null);
	}

	static String getInternalSellerName(InternFaktura internFaktura) {
		return internFaktura.sellerInformationFirstName() + " " + internFaktura.sellerInformationLastName();
	}

}
