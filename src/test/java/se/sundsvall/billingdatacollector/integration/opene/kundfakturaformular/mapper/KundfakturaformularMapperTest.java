package se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.mapper;

import static generated.se.sundsvall.billingpreprocessor.Status.APPROVED;
import static generated.se.sundsvall.billingpreprocessor.Type.EXTERNAL;
import static generated.se.sundsvall.billingpreprocessor.Type.INTERNAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static se.sundsvall.billingdatacollector.TestDataFactory.readBytesFromOpenEFile;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.TestDataFactory;
import se.sundsvall.billingdatacollector.support.annotation.UnitTest;

@SpringBootTest(classes = Application.class)
@UnitTest
class KundfakturaformularMapperTest {

	@Autowired
	private KundfakturaformularMapper mapper;

	@Test
	void getSupportedFamilyId() {
		assertThat(mapper.getSupportedFamilyId()).isEqualTo("198");
	}

	@Test
	void mapToInternalOrganizationBillingRecord() {
		var wrapper = mapper.mapToBillingRecordWrapper(readBytesFromOpenEFile("flow-instance.internal.organization.xml"));

		// Assert the BillingRecordWrapper fields
		assertThat(wrapper.getFlowInstanceId()).isEqualTo("6859");
		assertThat(wrapper.getFamilyId()).isEqualTo("123");
		assertThat(wrapper.getLegalId()).isEqualTo("2120002411");

		var billingRecord = wrapper.getBillingRecord();
		assertThat(billingRecord).isNotNull();
		assertThat(billingRecord.getCategory()).isEqualTo("CUSTOMER_INVOICE");
		assertThat(billingRecord.getType()).isEqualTo(INTERNAL);
		assertThat(billingRecord.getStatus()).isEqualTo(APPROVED);
		assertThat(billingRecord.getApprovedBy()).isEqualTo("E_SERVICE");

		var recipient = billingRecord.getRecipient();
		assertThat(recipient).isNotNull();
		assertThat(recipient.getLegalId()).isEqualTo("2120002411");
		assertThat(recipient.getOrganizationName()).isEqualTo("Sundsvalls Kommun");

		var invoice = billingRecord.getInvoice();
		assertThat(invoice).isNotNull();
		assertThat(invoice.getCustomerId()).isEqualTo("15");
		assertThat(invoice.getDescription()).isEqualTo("Kundfaktura");
		assertThat(invoice.getOurReference()).isEqualTo("Kalle Anka");
		assertThat(invoice.getCustomerReference()).isEqualTo("1JAN16LAN");
		assertThat(invoice.getReferenceId()).isEqualTo("6859");

		var invoiceRows = invoice.getInvoiceRows();

		assertThat(invoiceRows).satisfiesExactlyInAnyOrder(
			row -> {
				assertThat(row.getDescriptions()).containsExactly("Bra fakturatext1");
				assertThat(row.getCostPerUnit()).isEqualTo(Float.parseFloat("567.89"));
				assertThat(row.getQuantity()).isEqualTo(Float.parseFloat("3.0"));
				assertThat(row.getAccountInformation().getCostCenter()).isEqualTo("15810100");
				assertThat(row.getAccountInformation().getSubaccount()).isEqualTo("931311");
				assertThat(row.getAccountInformation().getDepartment()).isEqualTo("510410");
				assertThat(row.getAccountInformation().getCounterpart()).isEqualTo("115");
			},
			row -> {
				assertThat(row.getDescriptions()).containsExactly("Bra fakturatext2");
				assertThat(row.getCostPerUnit()).isEqualTo(Float.parseFloat("234.56"));
				assertThat(row.getQuantity()).isEqualTo(Float.parseFloat("1.0"));
				assertThat(row.getAccountInformation().getCostCenter()).isEqualTo("11200100");
				assertThat(row.getAccountInformation().getSubaccount()).isEqualTo("930110");
				assertThat(row.getAccountInformation().getDepartment()).isEqualTo("100500");
				assertThat(row.getAccountInformation().getCounterpart()).isEqualTo("115");
			});
	}

	@Test
	void mapToExternalOrganizationBillingRecord() {
		var openEFile = readBytesFromOpenEFile("flow-instance.external.organization.xml");
		var billingRecordWrapper = mapper.mapToBillingRecordWrapper(openEFile);

		assertThat(billingRecordWrapper.getFamilyId()).isEqualTo("358");
		assertThat(billingRecordWrapper.getFlowInstanceId()).isEqualTo("6931");
		assertThat(billingRecordWrapper.getLegalId()).isEqualTo("5591628770");

		var billingRecord = billingRecordWrapper.getBillingRecord();
		assertThat(billingRecord).isNotNull();
		assertThat(billingRecord.getCategory()).isEqualTo("CUSTOMER_INVOICE");
		assertThat(billingRecord.getType()).isEqualTo(EXTERNAL);
		assertThat(billingRecord.getStatus()).isEqualTo(APPROVED);
		assertThat(billingRecord.getApprovedBy()).isEqualTo("E_SERVICE");

		var recipient = billingRecord.getRecipient();
		assertThat(recipient.getOrganizationName()).isEqualTo("5591628770");

		var addressDetails = recipient.getAddressDetails();
		assertThat(addressDetails.getStreet()).isEqualTo("Ankeborgsvägen 123");
		assertThat(addressDetails.getPostalCode()).isEqualTo("123 45");
		assertThat(addressDetails.getCity()).isEqualTo("Ankeborg");

		var invoice = billingRecord.getInvoice();
		assertThat(invoice.getDescription()).isEqualTo("Kundfaktura");
		assertThat(invoice.getOurReference()).isEqualTo("Kalle Anka");
		assertThat(invoice.getReferenceId()).isEqualTo("6931");

		var invoiceRows = invoice.getInvoiceRows();
		assertThat(invoiceRows).hasSize(1);

		var invoiceRow = invoiceRows.getFirst();
		assertThat(invoiceRow.getDescriptions()).containsExactly("Oerhört bra fakturatext");
		assertThat(invoiceRow.getVatCode()).isEqualTo("25");
		assertThat(invoiceRow.getCostPerUnit()).isEqualTo(12.34f);
		assertThat(invoiceRow.getQuantity()).isEqualTo(1.0f);

		var accountInformation = invoiceRow.getAccountInformation();
		assertThat(accountInformation.getCostCenter()).isEqualTo("66028000");
		assertThat(accountInformation.getSubaccount()).isEqualTo("313210");
		assertThat(accountInformation.getDepartment()).isEqualTo("315400");
		assertThat(accountInformation.getCounterpart()).isEqualTo("87000000");
	}

	@Test
	void mapToExternalPersonBillingRecord() {
		var openEFile = readBytesFromOpenEFile("flow-instance.external.person.xml");
		var billingRecordWrapper = mapper.mapToBillingRecordWrapper(openEFile);

		assertThat(billingRecordWrapper.getFamilyId()).isEqualTo("358");
		assertThat(billingRecordWrapper.getFlowInstanceId()).isEqualTo("225965");
		assertThat(billingRecordWrapper.getLegalId()).isEqualTo("190101011234");

		var billingRecord = billingRecordWrapper.getBillingRecord();
		assertThat(billingRecord.getCategory()).isEqualTo("CUSTOMER_INVOICE");
		assertThat(billingRecord.getType()).isEqualTo(EXTERNAL);
		assertThat(billingRecord.getStatus()).isEqualTo(APPROVED);
		assertThat(billingRecord.getApprovedBy()).isEqualTo("E_SERVICE");

		var recipient = billingRecord.getRecipient();
		assertThat(recipient.getFirstName()).isEqualTo("Kajsa");
		assertThat(recipient.getLastName()).isEqualTo("Anka");

		var addressDetails = recipient.getAddressDetails();
		assertThat(addressDetails.getStreet()).isEqualTo("Sundsvall 222");
		assertThat(addressDetails.getPostalCode()).isEqualTo("123 45");
		assertThat(addressDetails.getCity()).isEqualTo("ANKEBORG");

		var invoice = billingRecord.getInvoice();
		assertThat(invoice.getCustomerId()).isEqualTo("190101011234");
		assertThat(invoice.getDescription()).isEqualTo("Kundfaktura");
		assertThat(invoice.getOurReference()).isEqualTo("Kalle Anka");
		assertThat(invoice.getCustomerReference()).isEqualTo("Kajsa Anka");
		assertThat(invoice.getReferenceId()).isEqualTo("225965");

		var invoiceRows = invoice.getInvoiceRows();
		assertThat(invoiceRows).hasSize(1);

		var invoiceRow = invoiceRows.getFirst();
		assertThat(invoiceRow.getDescriptions()).containsExactly("Slutfaktura 1st. pengavalv");
		assertThat(invoiceRow.getVatCode()).isEqualTo("25");
		assertThat(invoiceRow.getCostPerUnit()).isEqualTo(24000.0f);
		assertThat(invoiceRow.getQuantity()).isEqualTo(1.0f);

		var accountInformation = invoiceRow.getAccountInformation();
		assertThat(accountInformation.getCostCenter()).isEqualTo("66050000");
		assertThat(accountInformation.getSubaccount()).isEqualTo("301100");
		assertThat(accountInformation.getDepartment()).isEqualTo("450220");
		assertThat(accountInformation.getActivity()).isEqualTo("6200");
		assertThat(accountInformation.getCounterpart()).isEqualTo("86000000");
	}

	@Test
	void testCheckForValidBillingRecords_shouldThrowException_whenIncompleteInvoiceRows() {
		var openEFile = readBytesFromOpenEFile("flow-instance.external.incomplete.xml");
		assertThatExceptionOfType(ThrowableProblem.class).isThrownBy(() -> mapper.mapToBillingRecordWrapper(openEFile))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getTitle()).isEqualTo("Missing information from OpenE");
				assertThat(throwableProblem.getDetail()).isEqualTo("Missing the following fields: [MomssatsExtern, MomssatsExtern, AnsvarExtern, VerksamhetExtern, UnderkontoExtern]");
			});
	}

	@Test
	void testParseList_faultyNamespace_shouldThrowException() {
		var openEFile = TestDataFactory.readBytesFromOpenEFile("flow-instance.faulty.namespace.xml");

		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mapper.mapToBillingRecordWrapper(openEFile))
			.satisfies(e -> assertThat(e.getMessage()).contains("Namespace mismatch: expected http://www.oeplatform.org/version/2.0/schemas/flowinstance but found http://www.nowhere.com"));
	}

	@Test
	void test() {
		// We should have bailed out before this, maybe corrupt xml could cause this exception though
		var openEFile = TestDataFactory.readBytesFromOpenEFile("flow-instance.404.xml");
		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> mapper.mapToBillingRecordWrapper(openEFile))
			.satisfies(e -> assertThat(e.getMessage()).contains("Error parsing xml"));
	}
}