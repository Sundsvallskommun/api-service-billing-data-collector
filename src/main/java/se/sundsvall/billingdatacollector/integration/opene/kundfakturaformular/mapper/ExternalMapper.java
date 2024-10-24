package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.APPROVED_BY;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.CATEGORY;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.INVOICE_DESCRIPTION;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper.KundfakturaformularMapper.MAX_DESCRIPTION_LENGTH;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.extractValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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

import generated.se.sundsvall.billingpreprocessor.AccountInformation;
import generated.se.sundsvall.billingpreprocessor.AddressDetails;
import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.InvoiceRow;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.billingpreprocessor.Status;
import generated.se.sundsvall.billingpreprocessor.Type;

@Component
public class ExternalMapper {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExternalMapper.class);

	private final MapperHelper mapperHelper;

	public ExternalMapper(MapperHelper mapperHelper) {
		this.mapperHelper = mapperHelper;
	}

	BillingRecordWrapper mapToExternalBillingRecord(final byte[] xml, OpeneCollections collections) {
		LOGGER.info("Mapping to external billing record");
		var result = extractValue(xml, ExternFaktura.class);

		mapperHelper.checkForNeededFieldsForExternal(collections);

		// Check if it's an external person or organization
		if(isBlank(result.referensForetag())) {
			LOGGER.info("Mapping to billing record for an external  person");
			return mapToExternalBillingRecordForPerson(result, collections);
		} else {
			LOGGER.info("Mapping to billing record for an external organization");
			return mapToExternalBillingRecordForOrganization(result, collections);
		}
	}

	BillingRecordWrapper mapToExternalBillingRecordForOrganization(ExternFaktura result, OpeneCollections collections) {
		LOGGER.info("All values for ExternFaktura: {}", result.organisationsInformation());

		var organizationInformation = mapperHelper.getOrganizationInformation(result.organisationsInformation());

		var billingRecord = new BillingRecord()
			.category(CATEGORY)
			.status(Status.APPROVED)
			.approvedBy(APPROVED_BY)
			.type(Type.EXTERNAL)
			.recipient(new Recipient()
				.organizationName(organizationInformation.getOrganizationNumber())
				.firstName(result.fornamn())
				.lastName(result.efternamn())
				.addressDetails(new AddressDetails()
					.careOf(organizationInformation.getCareOf())
					.street(organizationInformation.getStreetAddress())
					.postalCode(organizationInformation.getZipCode())
					.city(organizationInformation.getCity())))
			.invoice(createExternalInvoice(result, collections, mapperHelper.getExternalMotpartNumbers(result.organisationsInformation())));

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(organizationInformation.getOrganizationNumber())
			.withFamilyId(result.familyId())
			.withFlowInstanceId(result.flowInstanceId())
			.build();
	}

	BillingRecordWrapper mapToExternalBillingRecordForPerson(ExternFaktura result, OpeneCollections collections) {
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
			.invoice(createExternalInvoice(result, collections, mapperHelper.getExternalMotpartNumbers(result.motpartNamn())));

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(result.personnummer())
			.withFamilyId(result.familyId())
			.withFlowInstanceId(result.flowInstanceId())
			.build();
	}

	Invoice createExternalInvoice(ExternFaktura externFaktura, OpeneCollections collections, String motpart) {
		return new Invoice()
			.customerReference(getExternalCustomerReference(externFaktura))
			.customerId(externFaktura.personnummer())
			.description(mapperHelper.truncateString(INVOICE_DESCRIPTION, MAX_DESCRIPTION_LENGTH))  //Cannot be more than 30 chars
			.ourReference(getExternalSellerName(externFaktura))
			.referenceId(externFaktura.flowInstanceId())
			.invoiceRows(createExternalInvoiceRows(collections, motpart));
	}

	List<InvoiceRow> createExternalInvoiceRows(OpeneCollections collections, String motpart) {
		List<InvoiceRow> invoiceRows = new ArrayList<>();

		for(int index = 1; index < collections.getNumberOfRows() + 1; index++ ) {
			LOGGER.info("Creating external invoice row for index: {}", index);
			var invoiceRow = new InvoiceRow()
				.descriptions(ofNullable(mapperHelper.truncateString(
					ofNullable(collections.getBerakningExternMap().get(index)).map(BerakningExtern::getFakturatextExtern).orElse(null), MAX_DESCRIPTION_LENGTH))
					.map(List::of)
					.orElseGet(Collections::emptyList))
				.quantity(mapperHelper.convertStringToFloat(
					ofNullable(collections.getBerakningExternMap().get(index)).map(BerakningExtern::getAntalExtern).orElse(null)))
				.costPerUnit(mapperHelper.convertStringToFloat(
					ofNullable(collections.getBerakningExternMap().get(index)).map(BerakningExtern::getAPrisExtern).orElse(null)))
				.vatCode(ofNullable(collections.getMomssatsExternMap().get(index)).map(MomssatsExtern::getValue).orElse(null))
				.accountInformation(new AccountInformation()
					.activity(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAktivitetskontoExternMap().get(index)).map(AktivitetskontoExtern::getValue).orElse(null)))
					.article(ofNullable(collections.getObjektKontoExternMap().get(index)).map(ObjektkontoExtern::getValue).orElse(null))
					.costCenter(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getAnsvarExternMap().get(index)).map(AnsvarExtern::getValue).orElse(null)))
					.counterpart(motpart)
					.department(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getVerksamhetExternMap().get(index)).map(VerksamhetExtern::getValue).orElse(null)))
					.subaccount(mapperHelper.getLeadingDigitsFromString(
						ofNullable(collections.getUnderkontoExternMap().get(index)).map(UnderkontoExtern::getValue).orElse(null))));
			invoiceRows.add(invoiceRow);
		}

		return invoiceRows;
	}

	String getExternalSellerName(ExternFaktura externFaktura) {
		return externFaktura.saljarensFornamn() + " " + externFaktura.saljarensEfternamn();
	}

	String getExternalCustomerReference(ExternFaktura externFaktura) {
		return externFaktura.kontaktuppgifterPrivatpersonFornamn() + " " + externFaktura.kontaktuppgifterPrivatpersonEfternamn();
	}


}
