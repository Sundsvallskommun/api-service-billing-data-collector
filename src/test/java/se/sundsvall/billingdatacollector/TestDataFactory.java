package se.sundsvall.billingdatacollector;

import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.BillingRecordConstants.SUNDSVALLS_MUNICIPALITY;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.BillingRecordConstants.SUNDSVALLS_MUNICIPALITY_ID;
import static se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.BillingRecordConstants.SUNDSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;

import se.sundsvall.billingdatacollector.integration.db.model.HistoryEntity;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledJobEntity;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import generated.se.sundsvall.billingpreprocessor.AccountInformation;
import generated.se.sundsvall.billingpreprocessor.AddressDetails;
import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import generated.se.sundsvall.billingpreprocessor.Invoice;
import generated.se.sundsvall.billingpreprocessor.InvoiceRow;
import generated.se.sundsvall.billingpreprocessor.Recipient;
import generated.se.sundsvall.billingpreprocessor.Status;
import generated.se.sundsvall.billingpreprocessor.Type;

public final class TestDataFactory {

	public static byte[] readBytesFromOpenEFile(String fileName) {
		Path path = Paths.get("src/test/resources/open-e/" + fileName);
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public static BillingRecordWrapper createKundfakturaBillingRecordWrapper(boolean internal) {
		final var wrapper = BillingRecordWrapper.builder()
			.withFamilyId("123")
			.withLegalId("1234567890")
			.withMunicipalityId(SUNDSVALLS_MUNICIPALITY_ID)
			.build();

		if (internal) {
			wrapper.setBillingRecord(createInternalBillingRecord());
		} else {
			wrapper.setBillingRecord(createExternalBillingRecord());
		}

		return wrapper;
	}

	public static BillingRecord createInternalBillingRecord() {
		return new BillingRecord()
			.category("KUNDFAKTURA")
			.status(Status.APPROVED)
			.type(Type.INTERNAL)
			.recipient(new Recipient()
				.organizationName(SUNDSVALLS_MUNICIPALITY)
				.legalId(SUNDSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER))
			.invoice(new Invoice()
				.customerId("customerId")
				.invoiceRows(List.of(new InvoiceRow()
					.descriptions(List.of("Invoice text"))
					.quantity(2f)
					.costPerUnit(150f)
					.totalAmount(300f)
					.accountInformation(new AccountInformation()
						.costCenter("16300000")
						.subaccount("936100")
						.department("910300")
						.activity("5247")
						.counterpart("170")))));
	}

	public static BillingRecord createExternalBillingRecord() {
		return new BillingRecord()
			.category("KUNDFAKTURA")
			.status(Status.APPROVED)
			.type(Type.EXTERNAL)
			.recipient(new Recipient()
				.firstName("firstName")
				.lastName("lastName")
				.addressDetails(new AddressDetails()
					.street("Something street")
					.postalCode("123 45")
					.city("TOWN")))
			.invoice(new Invoice()
				.invoiceRows(List.of(new InvoiceRow()
					.descriptions(List.of("Invoice text"))
					.quantity(3f)
					.costPerUnit(100f)
					.vatCode("00")
					.totalAmount(300f)
					.accountInformation(new AccountInformation()
						.costCenter("43200000")
						.subaccount("345000")
						.department("315310")
						.activity("4165")
						.article("3452000 - GULLGÅRDEN")
						.counterpart("86000000")))));
	}

	public static HistoryEntity createHistoryEntity(String flowInstanceId) {
		final HistoryEntity historyEntity = new HistoryEntity();
		historyEntity.setFlowInstanceId(flowInstanceId);
		return historyEntity;
	}

	public static ScheduledJobEntity createScheduledJobEntity() {
		final ScheduledJobEntity scheduledJobEntity = new ScheduledJobEntity();
		scheduledJobEntity.setFetchedStartDate(LocalDate.now().minusDays(4));
		scheduledJobEntity.setFetchedEndDate(LocalDate.now().minusDays(3));
		return scheduledJobEntity;
	}
}
