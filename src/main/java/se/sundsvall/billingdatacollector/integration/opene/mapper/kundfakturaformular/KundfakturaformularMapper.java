package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.KUNDFAKTURA_FORMULAR_FAMILY_ID;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNDSVALLS_MUNICIPALIY;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.extractValue;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.getString;

import java.util.List;

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
	public BillingRecordWrapper mapToBillingRecordWrapper(final byte[] xml) {
		if (getString(xml, "/FlowInstance/Values/BarakningarExtern1") == null) {
			return mapToInternalBillingRecord(xml);
		} else {
			return mapToExternalBillingRecord(xml);
		}
	}

	BillingRecordWrapper mapToInternalBillingRecord(final byte[] xml) {
		var result = extractValue(xml, InternFaktura.class);

		// Get the customerId, we will reuse this and add a "1" in front of it to create the counterpart
		var customerId = mapperHelper.getLeadingDigits(result.forvaltningSomSkaBetala());

		var billingRecord = new BillingRecord()
			.category(CATEGORY)
			.status(Status.APPROVED)
			.type(Type.INTERNAL)
			.recipient(new Recipient()
				.organizationName(SUNDSVALLS_MUNICIPALIY)
				.legalId(SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER))
			.invoice(new Invoice()
				.customerId(customerId)
				.referenceId(result.flowInstanceId())
				.invoiceRows(List.of(new InvoiceRow()
					.descriptions(List.of(result.fakturaText()))
					.quantity(result.antal())
					.costPerUnit(mapperHelper.convertStringToFloat(result.aPris()))
					.totalAmount(mapperHelper.convertStringToFloat(result.summering()))
					.accountInformation(new AccountInformation()
						.costCenter(mapperHelper.getLeadingDigits(result.ansvar()))
						.subaccount(mapperHelper.getLeadingDigits(result.underkonto()))
						.department(mapperHelper.getLeadingDigits(result.verksamhet()))
						.activity(mapperHelper.getLeadingDigits(result.aktivitetskonto()))
						.counterpart(mapperHelper.getInternalMotpartNumbers(customerId))))));

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withFlowInstanceId(result.flowInstanceId())
			.build();
	}

	BillingRecordWrapper mapToExternalBillingRecord(final byte[] xml) {
		var result = extractValue(xml, ExternFaktura.class);

		var billingRecord = new BillingRecord()
			.category(CATEGORY)
			.status(Status.APPROVED)
			.type(Type.EXTERNAL)
			.recipient(new Recipient()
				.firstName(result.fornamn())
				.lastName(result.efternamn())
				.addressDetails(new AddressDetails()
					.street(result.adress())
					.postalCode(result.postnummer())
					.city(result.ort())))
			.invoice(new Invoice()
				.referenceId(result.flowInstanceId())
				.invoiceRows(List.of(new InvoiceRow()
					.descriptions(List.of(result.fakturaText()))
					.quantity(result.antal())
					.costPerUnit(mapperHelper.convertStringToFloat(result.aPris()))
					.vatCode(result.momssats())
					.totalAmount(mapperHelper.convertStringToFloat(result.summering()))
					.accountInformation(new AccountInformation()
						.costCenter(mapperHelper.getLeadingDigits(result.ansvar()))
						.subaccount(mapperHelper.getLeadingDigits(result.underkonto()))
						.department(mapperHelper.getLeadingDigits(result.verksamhet()))
						.activity(mapperHelper.getLeadingDigits(result.aktivitetskonto()))
						.article(result.objektkonto())
						.counterpart(mapperHelper.getExternalMotpartNumbers(result.motpartNamn()))))));

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(result.personnummer())
			.withFlowInstanceId(result.flowInstanceId())
			.build();
	}
}
