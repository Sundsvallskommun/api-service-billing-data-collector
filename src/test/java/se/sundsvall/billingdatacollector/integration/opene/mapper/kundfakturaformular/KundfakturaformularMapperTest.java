package se.sundsvall.billingdatacollector.integration.opene.mapper.kundfakturaformular;

import static org.assertj.core.api.Assertions.assertThat;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNDSVALLS_MUNICIPALITY;
import static se.sundsvall.billingdatacollector.integration.opene.mapper.BillingRecordConstants.SUNSVALLS_MUNICIPALITY_ORGANIZATION_NUMBER;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.kubernetes.commons.loadbalancer.ConditionalOnKubernetesLoadBalancerEnabled;
import org.springframework.test.context.ActiveProfiles;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegrationProperties;
import se.sundsvall.billingdatacollector.integration.opene.mapper.MapperHelper;
import se.sundsvall.billingdatacollector.integration.opene.util.ListUtil;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

import generated.se.sundsvall.billingpreprocessor.Status;
import generated.se.sundsvall.billingpreprocessor.Type;

@ExtendWith(ResourceLoaderExtension.class)
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
	void mapToInternalBillingRecord(@Load(value = "/open-e/flow-instance.internal.xml", as = Load.ResourceType.STRING) String xml) {
		var billingRecordWrapper = mapper.mapToBillingRecordWrapper(xml.getBytes(StandardCharsets.UTF_8));
		System.out.println(billingRecordWrapper);
	}

	@Test
	void mapToExternalBillingRecord(@Load(value = "/open-e/flow-instance.external.xml", as = Load.ResourceType.STRING) String xml) {
		var billingRecordWrapper = mapper.mapToBillingRecordWrapper(xml.getBytes(StandardCharsets.UTF_8));
		System.out.println(billingRecordWrapper);

	}

	@MethodSource("provideStringLength")
	@ParameterizedTest
	void testTruncateString(String input, String wanted, int maxLength) {
		assertThat(mapper.truncateString(input, maxLength)).isEqualTo(wanted);
	}

	private static Stream<Arguments> provideStringLength() {
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
