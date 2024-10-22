package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import se.sundsvall.billingdatacollector.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class KundfakturaformularMapperTest {

	@Autowired
	private KundfakturaformularMapper mapper;

	@Test
	void getSupportedFamilyId() {
		assertThat(mapper.getSupportedFamilyId()).isEqualTo("198");
	}

	@Test
	void mapToInternalOrganizationBillingRecord() {
		var billingRecordWrapper = mapper.mapToBillingRecordWrapper(readOpenEFile("flow-instance.internal.organization.xml"));
	}

	@Test
	void mapToExternalOrganizationBillingRecord() {
		var billingRecordWrapper = mapper.mapToBillingRecordWrapper(readOpenEFile("flow-instance.external.organization.xml"));
	}

	@Test
	void mapToExternalPersonBillingRecord() {
		var billingRecordWrapper = mapper.mapToBillingRecordWrapper(readOpenEFile("flow-instance.external.person.xml"));
	}

	@Test
	void test() {
		mapper.mapToBillingRecordWrapper(readOpenEFile("test.xml"));
	}

	@MethodSource("provideStringsForTruncation")
	@ParameterizedTest
	void testTruncateString(String input, String wanted, int maxLength) {
		assertThat(mapper.truncateString(input, maxLength)).isEqualTo(wanted);
	}

	private static Stream<Arguments> provideStringsForTruncation() {
		return Stream.of(
			// (input, wanted, maxLength)
			Arguments.of(null, null, 5),
			Arguments.of("", "", 5),
			Arguments.of(" ", " ", 5),
			Arguments.of("abcd", "abcd", 5),
			Arguments.of("abcde", "abcde", 5),
			Arguments.of("abcdef", "abcde", 5)
		);
	}

	//Not using "@Resourceloader" because it's not compatible with ISO-8859-1.
	private byte[] readOpenEFile(String fileName) {
		Path path = Paths.get("src/test/resources/open-e/" + fileName);
		try {
			return Files.readAllBytes(path);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
