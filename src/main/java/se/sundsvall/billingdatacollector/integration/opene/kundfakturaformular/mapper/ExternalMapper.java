package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.APPROVED_BY;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.CATEGORY;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.INVOICE_DESCRIPTION;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.MAX_DESCRIPTION_LENGTH;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.extractValue;

import generated.se.sundsvall.billingpreprocessor.AccountInformation;
import generated.se.sundsvall.billingpreprocessor.AddressDetails;
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
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.ExternFaktura;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.OpeneCollections;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.AktivitetskontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.AnsvarExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.BerakningExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.MomssatsExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.ObjektkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.UnderkontoExtern;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.external.VerksamhetExtern;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

final class ExternalMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalMapper.class);

	private ExternalMapper() {
		// Not meant to be instantiated
	}

	static BillingRecordWrapper mapToExternalBillingRecord(final byte[] xml, OpeneCollections collections) {
		LOGGER.info("Mapping to external billing record");
		var result = extractValue(xml, ExternFaktura.class);

		MapperHelper.checkForNeededFieldsForExternal(collections);

		// Check if it's an external person or organization
		if (isBlank(result.referenceOrganization())) {
			LOGGER.info("Mapping to external billing record for a person");
			return mapToExternalBillingRecordForPerson(result, collections);
		}

		LOGGER.info("Mapping to external billing record for an organization");
		return mapToExternalBillingRecordForOrganization(result, collections);
	}

	static BillingRecordWrapper mapToExternalBillingRecordForOrganization(ExternFaktura result, OpeneCollections collections) {
		var organizationInformation = MapperHelper.getOrganizationInformation(result.organizationInformation());

		var billingRecord = new BillingRecord()
			.category(CATEGORY)
			.status(Status.APPROVED)
			.approvedBy(APPROVED_BY)
			.type(Type.EXTERNAL)
			.recipient(new Recipient()
				.organizationName(organizationInformation.getName())
				.addressDetails(new AddressDetails()
					.careOf(organizationInformation.getCareOf())
					.street(organizationInformation.getStreetAddress())
					.postalCode(organizationInformation.getZipCode())
					.city(organizationInformation.getCity())))
			.invoice(createExternalInvoice(result, collections, MapperHelper.getExternalMotpartNumbers(result.organizationInformation()), organizationInformation.getOrganizationNumber()));

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(organizationInformation.getOrganizationNumber())
			.withFamilyId(result.familyId())
			.withFlowInstanceId(result.flowInstanceId())
			.build();
	}

	static BillingRecordWrapper mapToExternalBillingRecordForPerson(ExternFaktura result, OpeneCollections collections) {
		var billingRecord = new BillingRecord()
			.category(CATEGORY)
			.status(Status.APPROVED)
			.approvedBy(APPROVED_BY)
			.type(Type.EXTERNAL)
			.recipient(new Recipient()
				.firstName(result.privatePersonFirstName())
				.lastName(result.privatePersonLastName())
				.addressDetails(new AddressDetails()
					.street(result.privatePersonAddress())
					.postalCode(result.privatePersonZipCode())
					.city(result.privatePersonPostalAddress())))
			.invoice(createExternalInvoice(result, collections, MapperHelper.getExternalMotpartNumbers(result.counterpartPrivatePersonName()), result.socialSecurityNumber()));

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(result.socialSecurityNumber())
			.withFamilyId(result.familyId())
			.withFlowInstanceId(result.flowInstanceId())
			.build();
	}

	static Invoice createExternalInvoice(ExternFaktura externFaktura, OpeneCollections collections, String motpart, String customerId) {
		return new Invoice()
			.customerReference(getExternalCustomerReference(externFaktura))
			.customerId(customerId)
			.description(MapperHelper.truncateString(INVOICE_DESCRIPTION, MAX_DESCRIPTION_LENGTH))  // Cannot be more than 30 chars
			.ourReference(getExternalSellerName(externFaktura))
			.referenceId(externFaktura.flowInstanceId())
			.invoiceRows(createExternalInvoiceRows(collections, motpart));
	}

	static List<InvoiceRow> createExternalInvoiceRows(OpeneCollections collections, String motpart) {
		List<InvoiceRow> invoiceRows = new ArrayList<>();

		for (int index = 1; index < collections.getNumberOfRows() + 1; index++) {   // OpenE-Arrays start at one...
			LOGGER.info("Creating external invoice row for index: {}", index);
			var invoiceRow = new InvoiceRow()
				.descriptions(ofNullable(MapperHelper.truncateString(
					ofNullable(collections.getBerakningExternMap().get(index)).map(BerakningExtern::getFakturatextExtern).orElse(null), MAX_DESCRIPTION_LENGTH))
					.map(List::of)
					.orElseGet(Collections::emptyList))
				.quantity(MapperHelper.convertStringToFloat(
					ofNullable(collections.getBerakningExternMap().get(index)).map(BerakningExtern::getAntalExtern).orElse(null)))
				.costPerUnit(MapperHelper.convertStringToFloat(
					ofNullable(collections.getBerakningExternMap().get(index)).map(BerakningExtern::getAPrisExtern).orElse(null)))
				.vatCode(ofNullable(collections.getMomssatsExternMap().get(index)).map(MomssatsExtern::getValue).orElse(null))
				.accountInformation(new AccountInformation()
					.activity(MapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAktivitetskontoExternMap().get(index)).map(AktivitetskontoExtern::getValue).orElse(null)))
					.article(ofNullable(collections.getObjektKontoExternMap().get(index)).map(ObjektkontoExtern::getValue).orElse(null))
					.costCenter(MapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAnsvarExternMap().get(index)).map(AnsvarExtern::getValue).orElse(null)))
					.counterpart(motpart)
					.department(MapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getVerksamhetExternMap().get(index)).map(VerksamhetExtern::getValue).orElse(null)))
					.subaccount(MapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getUnderkontoExternMap().get(index)).map(UnderkontoExtern::getValue).orElse(null))));
			invoiceRows.add(invoiceRow);
		}

		return invoiceRows;
	}

	static String getExternalSellerName(ExternFaktura externFaktura) {
		return externFaktura.sellerInformationFirstName() + " " + externFaktura.sellerInformationLastName();
	}

	static String getExternalCustomerReference(ExternFaktura externFaktura) {
		return externFaktura.privatePersonFirstName() + " " + externFaktura.privatePersonLastName();
	}

}
