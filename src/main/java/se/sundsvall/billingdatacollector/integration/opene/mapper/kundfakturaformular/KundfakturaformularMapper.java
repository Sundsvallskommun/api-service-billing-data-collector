package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.KUNDFAKTURA_FORMULAR_FAMILY_ID;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNDSVALLS_MUNICIPALIY;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.extractValue;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.getString;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import se.sundsvall.billingdatacollector.integration.opene.OpenEMapper;
import se.sundsvall.billingdatacollector.integration.opene.mapper.MapperHelper;
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

	private static final Logger LOG = LoggerFactory.getLogger(KundfakturaformularMapper.class);

	private static final String CATEGORY = "KUNDFAKTURA";

	private final MapperHelper mapperHelper;

	KundfakturaformularMapper(MapperHelper mapperHelper) {
		this.mapperHelper = mapperHelper;
	}

	@Override
	public String getSupportedFamilyId() {
		return KUNDFAKTURA_FORMULAR_FAMILY_ID;
	}

	@Override
	public BillingRecordWrapper mapToBillingRecord(final byte[] xml) {
		if (getString(xml, "/FlowInstance/Values/BarakningarExtern1") == null) {
			return mapToInternalBillingRecord(xml);
		} else {
			return mapToExternalBillingRecord(xml);
		}
	}

	BillingRecordWrapper mapToInternalBillingRecord(final byte[] xml) {
		var result = extractValue(xml, InternFaktura.class);

		//Get the customerId, we will reuse this and add a "1" in front of it to create the counterpart
		var customerId = mapperHelper.getLeadingDigits(result.forvaltningSomSkaBetala());

		var billingRecord = BillingRecord.builder()
			.withCategory(CATEGORY)
			.withStatus(Status.APPROVED)
			.withType(Type.INTERNAL)
			.withRecipient(Recipient.builder()
				.withOrganizationName(SUNDSVALLS_MUNICIPALIY)
				.withLegalId(SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER)
				.build())
			.withInvoice(Invoice.builder()
				.withCustomerId(customerId)
				.withInvoiceRows(List.of(InvoiceRow.builder()
					.withDescriptions(List.of(result.fakturaText()))
					.withQuantity(result.antal())
					.withCostPerUnit(mapperHelper.convertStringToFloat(result.aPris()))
					.withTotalAmount(mapperHelper.convertStringToFloat(result.summering()))
					.withAccountInformation(AccountInformation.builder()
						.withCostCenter(mapperHelper.getLeadingDigits(result.ansvar()))
						.withSubaccount(mapperHelper.getLeadingDigits(result.underkonto()))
						.withDepartment(mapperHelper.getLeadingDigits(result.verksamhet()))
						.withActivity(mapperHelper.getLeadingDigits(result.aktivitetskonto()))
						.withCounterpart(mapperHelper.getInternalMotpartNumbers(customerId))
						.build())
					.build()))
				.build())
			.build();

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.build();
	}

	BillingRecordWrapper mapToExternalBillingRecord(final byte[] xml) {
		var result = extractValue(xml, ExternFaktura.class);

		var billingRecord = BillingRecord.builder()
			.withCategory(CATEGORY)
			.withStatus(Status.APPROVED)
			.withType(Type.EXTERNAL)
			.withRecipient(Recipient.builder()
				.withFirstName(result.fornamn())
				.withLastName(result.efternamn())
				.withAddressDetails(AddressDetails.builder()
					.withStreet(result.adress())
					.withPostalCode(result.postnummer())
					.withCity(result.ort())
					.build())
				.build())
			.withInvoice(Invoice.builder()
				.withInvoiceRows(List.of(InvoiceRow.builder()
					.withDescriptions(List.of(result.fakturaText()))
					.withQuantity(result.antal())
					.withCostPerUnit(mapperHelper.convertStringToFloat(result.aPris()))
					.withVatCode(result.momssats())
					.withTotalAmount(mapperHelper.convertStringToFloat(result.summering()))
					.withAccountInformation(AccountInformation.builder()
						.withCostCenter(mapperHelper.getLeadingDigits(result.ansvar()))
						.withSubaccount(mapperHelper.getLeadingDigits(result.underkonto()))
						.withDepartment(mapperHelper.getLeadingDigits(result.verksamhet()))
						.withActivity(mapperHelper.getLeadingDigits(result.aktivitetskonto()))
						.withArticle(result.objektkonto())
						.withCounterpart(mapperHelper.getExternalMotpartNumbers(result.motpartNamn()))
						.build())
					.build()))
				.build())
			.build();

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(result.personnummer())
			.build();
	}
}
