package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNDSVALLS_MUNICIPALIY;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.zalando.problem.ThrowableProblem;

import generated.se.sundsvall.billingpreprocessor.Status;
import generated.se.sundsvall.billingpreprocessor.Type;

class KundfakturaformularMapperTests {

	private final KundfakturaformularMapper mapper = new KundfakturaformularMapper();

	@Test
	void getSupportedFamilyId() {
		assertThat(mapper.getSupportedFamilyId()).isEqualTo("358");
	}

	@Test
	void mapToInternalBillingRecord() {
		var stringBytes = readOpenEFile("flow-instance.internal.xml");
		var billingRecord = mapper.mapToBillingRecord(stringBytes).getBillingRecord();

		assertThat(billingRecord).isNotNull();
		assertThat(billingRecord.getCategory()).isEqualTo("KUNDFAKTURA");
		assertThat(billingRecord.getStatus()).isEqualTo(Status.APPROVED);
		assertThat(billingRecord.getType()).isEqualTo(Type.INTERNAL);

		var recipient = billingRecord.getRecipient();
		assertThat(recipient.getOrganizationName()).isEqualTo(SUNDSVALLS_MUNICIPALIY);
		assertThat(recipient.getLegalId()).isEqualTo(SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER);

		var invoice = billingRecord.getInvoice();
		assertThat(invoice.getCustomerId()).isEqualTo("70");

		var invoiceRows = invoice.getInvoiceRows();
		assertThat(invoiceRows).isNotNull().hasSize(1);

		//There can be only one
		var invoiceRow = invoiceRows.getFirst();
		assertThat(invoiceRow.getDescriptions()).contains("Passerkort med foto Pelle Lundströmsson enl E-tjänst 161342");
		assertThat(invoiceRow.getQuantity()).isOne();
		assertThat(invoiceRow.getCostPerUnit()).isEqualTo(150.0f);
		assertThat(invoiceRow.getTotalAmount()).isEqualTo(150.0f);

		var accountInformation = invoiceRow.getAccountInformation();
		assertThat(accountInformation.getCostCenter()).isEqualTo("16300000");
		assertThat(accountInformation.getSubaccount()).isEqualTo("936100");
		assertThat(accountInformation.getDepartment()).isEqualTo("910300");
		assertThat(accountInformation.getActivity()).isEqualTo("5247");
		assertThat(accountInformation.getCounterpart()).isEqualTo("170");
	}

	@Test
	void mapToExternalBillingRecord() {
		var stringBytes = readOpenEFile("flow-instance.external.xml");
		var billingRecord = mapper.mapToBillingRecord(stringBytes).getBillingRecord();
		assertThat(billingRecord.getCategory()).isEqualTo("KUNDFAKTURA");
		assertThat(billingRecord.getStatus()).isEqualTo(Status.APPROVED);
		assertThat(billingRecord.getType()).isEqualTo(Type.EXTERNAL);

		var recipient = billingRecord.getRecipient();
		assertThat(recipient.getFirstName()).isEqualTo("Nisse");
		assertThat(recipient.getLastName()).isEqualTo("Nilssonkvist");

		var addressDetails = recipient.getAddressDetails();
		assertThat(addressDetails.getStreet()).isEqualTo("Hittepågatan 12");
		assertThat(addressDetails.getPostalCode()).isEqualTo("123 45");
		assertThat(addressDetails.getCity()).isEqualTo("ORTEN");

		var invoice = billingRecord.getInvoice();
		assertThat(invoice).isNotNull();

		var invoiceRows = invoice.getInvoiceRows();
		assertThat(invoiceRows).hasSize(1);

		var invoiceRow = invoiceRows.getFirst();
		assertThat(invoiceRow.getDescriptions().getFirst()).isEqualTo("Påskmarknad. 3 marknadsplatser");
		assertThat(invoiceRow.getTotalAmount()).isEqualTo(2100f);
		assertThat(invoiceRow.getVatCode()).isEqualTo("00");
		assertThat(invoiceRow.getCostPerUnit()).isEqualTo(700f);
		assertThat(invoiceRow.getQuantity()).isEqualTo(3);

		var accountInformation = invoiceRow.getAccountInformation();
		assertThat(accountInformation.getCostCenter()).isEqualTo("43200000");
		assertThat(accountInformation.getSubaccount()).isEqualTo("345000");
		assertThat(accountInformation.getDepartment()).isEqualTo("315310");
		assertThat(accountInformation.getActivity()).isEqualTo("4165");
		assertThat(accountInformation.getArticle()).isEqualTo("3452000 - GULLGÅRDEN");
		assertThat(accountInformation.getCounterpart()).isEqualTo("86000000");
	}

	@Test
	void convertStringToFlot_shouldThrowException_whenNotParseableToFloat() {
		var unparseable = "not a float";

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.convertStringToFloat(unparseable))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't convert 'not a float' to float");
			});
	}

	@Test
	void testGetLeadingDigits_shouldThrowException_whenNoDigitsFound() {
		var noDigits = "no leading digits";

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.getLeadingDigits(noDigits))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't extract leading digits from 'no leading digits'");
			});
	}

	@Test
	void testGetTrailingDigits_shouldThrowException_whenNoDigitsFound() {
		var noDigits = "no trailing digits";

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.getTrailingDigitsFromString(noDigits))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't extract trailing digits from 'no trailing digits'");
			});
	}

	@Test
	void testGetInternalMotpartNumbers_shouldThrowException_whenCustomerIdIsNull() {
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> mapper.getInternalMotpartNumbers(null))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't extract motpart numbers from 'null'");
			});
	}

	//Not using resourceloader because it's not compatible with ISO-8859-1.
	private byte[] readOpenEFile(String fileName) {
		Path path = Paths.get("src/test/resources/open-e/" + fileName);
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
