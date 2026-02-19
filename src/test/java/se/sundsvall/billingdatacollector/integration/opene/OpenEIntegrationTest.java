package se.sundsvall.billingdatacollector.integration.opene;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.billingdatacollector.service.DbService;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({
	MockitoExtension.class, ResourceLoaderExtension.class
})
class OpenEIntegrationTest {

	@Mock
	private OpenEClient mockOpenEClient;

	@Mock
	private OpenEMapper mockMapper;

	@Mock
	private DbService mockDbService;

	private OpenEIntegration openEIntegration;

	@BeforeEach
	void setUp() {
		when(mockMapper.getSupportedFamilyId()).thenReturn("123");

		openEIntegration = new OpenEIntegration(mockOpenEClient, List.of(mockMapper), mockDbService);
	}

	@Test
	void testGetFlowInstanceIds(@Load("/open-e/flow-instances.xml") final String xml) {
		when(mockOpenEClient.getErrands("123", "2024-04-25", "2024-04-25")).thenReturn(xml.getBytes(ISO_8859_1));

		final var result = openEIntegration.getFlowInstanceIds("123", "2024-04-25", "2024-04-25");
		assertThat(result).isNotNull().containsExactlyInAnyOrder("123456", "234567", "345678");

		verify(mockOpenEClient).getErrands("123", "2024-04-25", "2024-04-25");
		verifyNoMoreInteractions(mockOpenEClient);
		verifyNoMoreInteractions(mockMapper);
	}

	@Test
	void testGetBillingRecord(@Load("/open-e/flow-instance.internal.organization.xml") final String xml) {
		when(mockOpenEClient.getErrand("123456")).thenReturn(xml.getBytes(ISO_8859_1));
		when(mockMapper.mapToBillingRecordWrapper(any(byte[].class))).thenReturn(BillingRecordWrapper.builder().build());

		final var result = openEIntegration.getBillingRecord("123456");
		assertThat(result).isNotNull();

		verify(mockOpenEClient).getErrand("123456");
		verify(mockMapper).getSupportedFamilyId();
		verify(mockMapper).mapToBillingRecordWrapper(any(byte[].class));
		verifyNoMoreInteractions(mockOpenEClient, mockMapper);
	}

	@Test
	void testGetGetBillingRecordWhenNoMatchingMapperExists(@Load("/open-e/flow-instance.external.organization.xml") final String xml) {
		when(mockOpenEClient.getErrand("123456")).thenReturn(xml.getBytes(ISO_8859_1));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> openEIntegration.getBillingRecord("123456"))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't map billing record from OpenE");
				assertThat(throwableProblem.getDetail()).startsWith("Unsupported familyId: 358");
			});

		verify(mockOpenEClient).getErrand("123456");
		verify(mockMapper).getSupportedFamilyId();
		verifyNoMoreInteractions(mockOpenEClient, mockMapper);
	}

	@Test
	void testGetBillingRecordWhenNoFamilyIdExists(@Load("/open-e/flow-instance.404.xml") final String xml) {
		when(mockOpenEClient.getErrand("123456")).thenReturn(xml.getBytes(ISO_8859_1));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> openEIntegration.getBillingRecord("123456"))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo("Couldn't map billing record from OpenE");
				assertThat(throwableProblem.getDetail()).startsWith("No familyId found in response");
			});

		verify(mockOpenEClient).getErrand("123456");
		verifyNoMoreInteractions(mockOpenEClient, mockMapper);
	}

	@Test
	void testGetBillingRecord_shouldSaveToFalloutTable_whenMappingFails(@Load("/open-e/flow-instance.external.incomplete.xml") final String xml) {
		// Arrange
		when(mockOpenEClient.getErrand("123456")).thenReturn(xml.getBytes(ISO_8859_1));

		// Act
		openEIntegration.getBillingRecord("123456");

		// Assert
		verify(mockOpenEClient).getErrand("123456");
		// Not too important to verify how it fails, just that it fails and is saved.
		verify(mockDbService).saveFailedFlowInstance(xml.getBytes(ISO_8859_1), "123456", "123", "2281",
			"Cannot invoke \"se.sundsvall.billingdatacollector.model.BillingRecordWrapper.setFamilyId(String)\" because \"billingRecordWrapper\" is null");
	}
}
