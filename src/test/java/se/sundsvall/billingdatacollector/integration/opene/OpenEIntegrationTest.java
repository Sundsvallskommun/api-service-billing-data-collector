package se.sundsvall.billingdatacollector.integration.opene;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

@ExtendWith({MockitoExtension.class, ResourceLoaderExtension.class})
class OpenEIntegrationTest {

	@Mock
	private OpenEClient mockOpenEClient;

	@Mock
	private OpenEMapper mockMapper;

	private OpenEIntegration openEIntegration;

	@BeforeEach
	void setUp() {
		when(mockMapper.getSupportedFamilyId()).thenReturn("123");

		openEIntegration = new OpenEIntegration(mockOpenEClient, List.of(mockMapper));
	}

	@Test
	void getFlowInstanceIds(@Load("/open-e/flow-instances.xml") final String xml) {
		when(mockOpenEClient.getErrands("123", "2024-04-25", "2024-04-25")).thenReturn(xml.getBytes(UTF_8));

		var result = openEIntegration.getFlowInstanceIds("123", "2024-04-25", "2024-04-25");
		assertThat(result).isNotNull().containsExactlyInAnyOrder("123456", "234567", "345678");

		verify(mockOpenEClient).getErrands("123", "2024-04-25", "2024-04-25");
		verifyNoMoreInteractions(mockOpenEClient);
		verifyNoMoreInteractions(mockMapper);
	}

	@Test
	void getErrand(@Load("/open-e/flow-instance.internal.xml") final String xml) {
		when(mockOpenEClient.getErrand("123456")).thenReturn(xml.getBytes(UTF_8));
		when(mockMapper.mapToBillingRecordWrapper(any())).thenReturn(BillingRecordWrapper.builder().build());

		var result = openEIntegration.getBillingRecord("123456");
		assertThat(result).isNotNull();
		// TODO: some additional assertions once some actual mapping exists

		verify(mockOpenEClient).getErrand("123456");
		verify(mockMapper).getSupportedFamilyId();
		verify(mockMapper).mapToBillingRecordWrapper(any());
		verifyNoMoreInteractions(mockOpenEClient, mockMapper);
	}

	@Test
	void getErrandWhenNoMatchingMapperExists(@Load("/open-e/flow-instance.external.xml") final String xml) {
		when(mockOpenEClient.getErrand("123456")).thenReturn(xml.getBytes(UTF_8));

		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> openEIntegration.getBillingRecord("123456"))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(Status.INTERNAL_SERVER_ERROR);
				assertThat(throwableProblem.getTitle()).isEqualTo(Status.INTERNAL_SERVER_ERROR.getReasonPhrase());
				assertThat(throwableProblem.getDetail()).startsWith("No mapper for familyId");
			});

		verify(mockOpenEClient).getErrand("123456");
		verify(mockMapper).getSupportedFamilyId();
		verifyNoMoreInteractions(mockOpenEClient, mockMapper);
	}
}
