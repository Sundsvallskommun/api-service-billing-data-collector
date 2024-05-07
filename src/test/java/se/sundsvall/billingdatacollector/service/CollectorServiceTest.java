package se.sundsvall.billingdatacollector.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.billingdatacollector.TestDataFactory;
import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorIntegration;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegration;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.billingdatacollector.service.mapper.BillingRecordDecorator;

@ExtendWith(MockitoExtension.class)
class CollectorServiceTest {

	@Mock
	private OpenEIntegration mockOpenEIntegration;

	@Mock
	private BillingPreprocessorIntegration mockBillingPreprocessorIntegration;

	@Mock
	private BillingRecordDecorator mockDecorator;

	private static final String FAMILY_ID = "123";

	private CollectorService collectorService;

	@BeforeEach
	void setUp() {
		when(mockDecorator.getSupportedFamilyId()).thenReturn(FAMILY_ID);
		collectorService = new CollectorService(mockOpenEIntegration, mockBillingPreprocessorIntegration, List.of(mockDecorator));
	}

	@Test
	void testSendBillingData() {
		//Arrange
		var billingRecordWrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		when(mockOpenEIntegration.getBillingRecord(FAMILY_ID)).thenReturn(billingRecordWrapper);
		doNothing().when(mockDecorator).decorate(any(BillingRecordWrapper.class));
		doNothing().when(mockBillingPreprocessorIntegration).createBillingRecord(any());

		//Act
		collectorService.sendBillingData("123");

		//Assert
		verify(mockOpenEIntegration).getBillingRecord(FAMILY_ID);
		verify(mockDecorator).decorate(billingRecordWrapper);
		verify(mockBillingPreprocessorIntegration).createBillingRecord(billingRecordWrapper.getBillingRecord());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDecorator, mockBillingPreprocessorIntegration);
	}
}
