package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static org.zalando.problem.Status.INTERNAL_SERVER_ERROR;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.KUNDFAKTURA_FORMULAR_FAMILY_ID;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNDSVALLS_MUNICIPALIY;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.extractValue;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.getString;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;

import se.sundsvall.billingdatacollector.integration.opene.OpenEMapper;
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

	private static final Pattern LEADING_DIGITS_PATTERN = Pattern.compile("^\\d+");
	private static final Pattern TRAILING_DIGITS_PATTERN = Pattern.compile("\\d+(?=\\D*$)");

	private static final String MOTPART_ERROR = "Couldn't extract motpart numbers from '%s'";
	private static final String TRAILING_DIGITS_ERROR = "Couldn't extract trailing digits from '%s'";
	private static final String LEADING_DIGITS_ERROR = "Couldn't extract leading digits from '%s'";
	private static final String STRING_TO_FLOAT_ERROR = "Couldn't convert '%s' to float";

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
		var customerId = getLeadingDigits(result.forvaltningSomSkaBetala());

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
					.withCostPerUnit(convertStringToFloat(result.aPris()))
					.withTotalAmount(convertStringToFloat(result.summering()))
					.withAccountInformation(AccountInformation.builder()
						.withCostCenter(getLeadingDigits(result.ansvar()))
						.withSubaccount(getLeadingDigits(result.underkonto()))
						.withDepartment(getLeadingDigits(result.verksamhet()))
						.withActivity(getLeadingDigits(result.aktivitetskonto()))
						.withCounterpart(getInternalMotpartNumbers(customerId))
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
					.withCostPerUnit(convertStringToFloat(result.aPris()))
					.withVatCode(result.momssats())
					.withTotalAmount(convertStringToFloat(result.summering()))
					.withAccountInformation(AccountInformation.builder()
						.withCostCenter(getLeadingDigits(result.ansvar()))
						.withSubaccount(getLeadingDigits(result.underkonto()))
						.withDepartment(getLeadingDigits(result.verksamhet()))
						.withActivity(getLeadingDigits(result.aktivitetskonto()))
						.withArticle(result.objektkonto())
						.withCounterpart(getExternalMotpartNumbers(result.motpartNamn()))
						.build())
					.build()))
				.build())
			.build();

		return BillingRecordWrapper.builder()
			.withBillingRecord(billingRecord)
			.withLegalId(result.personnummer())
			.build();
	}

	//////
	// Lots of converters and stuff down here.
	// Move to a separate class if we need them in more places in the future.
	//////

	/**
	 * Converts a string to a float.
	 * e.g. "123,45 SEK" will be converted to Float: 123.45
	 * @param stringToConvert The string to convert
	 * @return The string converted to a float
	 */
	Float convertStringToFloat(String stringToConvert) {
		try {
			var withRemovedCurrency = removeCurrencyFromString(stringToConvert);
			var withReplacedCommas = replaceCommasInCurrencyString(withRemovedCurrency);
			return Float.parseFloat(withReplacedCommas);
		} catch (Exception e) {
			//Catch all the errors..
			throw Problem.builder()
				.withTitle(String.format(STRING_TO_FLOAT_ERROR, stringToConvert))
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	/**
	 * Replaces commas with dots in a currency string to be able to parse it to a Float
	 * @param stringToParse The string to parse
	 * @return The string with commas replaced with dots (if any).
	 */
	String replaceCommasInCurrencyString(String stringToParse) {
		return stringToParse.replace(",", ".");
	}

	/**
	 * Removes all characters that are not numbers, commas or dots.
	 * @param stringToParse The string to parse
	 * @return The string with all characters that are not numbers, commas or dots removed.
	 */
	String removeCurrencyFromString(String stringToParse) {
		return stringToParse.replaceAll("[^0-9.,]", "");
	}

	/**
	 * Extracts leading digits from a string.
	 * e.g. "123 - Something 456" will return "123"
	 * @param stringToParse The string to parse
	 * @return Any leading numbers from the string
	 */
	String getLeadingDigits(String stringToParse) {
		var matcher = LEADING_DIGITS_PATTERN.matcher(stringToParse);
		if (matcher.find()) {
			return matcher.group();
		} else {
			throw Problem.builder()
				.withTitle(String.format(LEADING_DIGITS_ERROR, stringToParse))
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	/**
	 * Extracts only trailing digits from a string.
	 * e.g. "123 - Something 456" will return "456"
	 * @param stringToParse The string to parse
	 * @return Any trailing numbers from the string
	 */
	String getTrailingDigitsFromString(String stringToParse) {
		var matcher = TRAILING_DIGITS_PATTERN.matcher(stringToParse);
		if (matcher.find()) {
			return matcher.group();
		} else {
			LOG.warn("Couldn't extract any trailing numbers from string: {}", stringToParse);
			throw Problem.builder()
				.withTitle(String.format(TRAILING_DIGITS_ERROR, stringToParse))
				.withStatus(INTERNAL_SERVER_ERROR)
				.build();
		}
	}

	/**
	 * Extracts the motpart/counterpart numbers from a string and fill with 0's up to 8 characters
	 * @param motpart The string to extract motpart numbers from
	 * @return The motpart numbers
	 */
	String getExternalMotpartNumbers(String motpart) {
		var numbers = getTrailingDigitsFromString(motpart);
		return StringUtils.rightPad(numbers, 8, "0");
	}

	String getInternalMotpartNumbers(String customerId) {
		return Optional.ofNullable(customerId).map(id -> "1" + id).orElseThrow(() -> Problem.builder()
			.withTitle(String.format(MOTPART_ERROR, customerId))
			.withStatus(INTERNAL_SERVER_ERROR)
			.build());
	}
}
