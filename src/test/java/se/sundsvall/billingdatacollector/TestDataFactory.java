package se.sundsvall.billingdatacollector;

import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNDSVALLS_MUNICIPALIY;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER;

import java.util.List;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import generated.se.sundsvall.billingpreprocessor.AccountInformation;
import generated.se.sundsvall.billingpreprocessor.AddressDetails;
import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.InvoiceRow;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.billingpreprocessor.Status;
import generated.se.sundsvall.billingpreprocessor.Type;

public class TestDataFactory {

	public static BillingRecordWrapper createKundfakturaBillingRecordWrapper(boolean internal) {
		var wrapper = BillingRecordWrapper.builder()
			.withFamilyId("123")
			.withLegalId("1234567890")
			.build();

		if (internal) {
			wrapper.setBillingRecord(createInternalBillingRecord());
		} else {
			wrapper.setBillingRecord(createExternalBillingRecord());
		}

		return wrapper;
	}

	public static BillingRecord createInternalBillingRecord() {
		return BillingRecord.builder()
			.withCategory("KUNDFAKTURA")
			.withStatus(Status.APPROVED)
			.withType(Type.INTERNAL)
			.withRecipient(Recipient.builder()
				.withOrganizationName(SUNDSVALLS_MUNICIPALIY)
				.withLegalId(SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER)
				.build())
			.withInvoice(Invoice.builder()
				.withCustomerId("customerId")
				.withInvoiceRows(List.of(InvoiceRow.builder()
					.withDescriptions(List.of("Invoice text"))
					.withQuantity(2)
					.withCostPerUnit(150f)
					.withTotalAmount(300f)
					.withAccountInformation(AccountInformation.builder()
						.withCostCenter("16300000")
						.withSubaccount("936100")
						.withDepartment("910300")
						.withActivity("5247")
						.withCounterpart("170")
						.build())
					.build()))
				.build())
			.build();
	}

	public static BillingRecord createExternalBillingRecord() {
		return BillingRecord.builder()
			.withCategory("KUNDFAKTURA")
			.withStatus(Status.APPROVED)
			.withType(Type.EXTERNAL)
			.withRecipient(Recipient.builder()
				.withFirstName("firstName")
				.withLastName("lastName")
				.withAddressDetails(AddressDetails.builder()
					.withStreet("Something street")
					.withPostalCode("123 45")
					.withCity("TOWN")
					.build())
				.build())
			.withInvoice(Invoice.builder()
				.withInvoiceRows(List.of(InvoiceRow.builder()
					.withDescriptions(List.of("Invoice text"))
					.withQuantity(3)
					.withCostPerUnit(100f)
					.withVatCode("00")
					.withTotalAmount(300f)
					.withAccountInformation(AccountInformation.builder()
						.withCostCenter("43200000")
						.withSubaccount("345000")
						.withDepartment("315310")
						.withActivity("4165")
						.withArticle("3452000 - GULLGÅRDEN")
						.withCounterpart("86000000")
						.build())
					.build()))
				.build())
			.build();
	}
}
