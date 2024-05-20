package se.sundsvall.billingdatacollector.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
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
import se.sundsvall.billingdatacollector.service.decorator.BillingRecordDecorator;

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
	void testTrigger() {
		//Arrange
		var billingRecordWrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		when(mockOpenEIntegration.getBillingRecord(FAMILY_ID)).thenReturn(billingRecordWrapper);
		doNothing().when(mockDecorator).decorate(any(BillingRecordWrapper.class));
		doNothing().when(mockBillingPreprocessorIntegration).createBillingRecord(any());

		//Act
		collectorService.trigger(FAMILY_ID);

		//Assert
		verify(mockOpenEIntegration).getBillingRecord(FAMILY_ID);
		verify(mockDecorator).decorate(billingRecordWrapper);
		verify(mockBillingPreprocessorIntegration).createBillingRecord(billingRecordWrapper.getBillingRecord());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDecorator, mockBillingPreprocessorIntegration);
	}

	@Test
	void testTriggerBillingBetweenDates() {
		//Arrange
		var startDate = LocalDate.now();
		var endDate = startDate.plusDays(1);
		var billingRecordWrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		when(mockOpenEIntegration.getSupportedFamilyIds()).thenReturn(List.of(FAMILY_ID));
		when(mockOpenEIntegration.getFlowInstanceIds(FAMILY_ID, startDate.toString(), endDate.toString())).thenReturn(List.of(FAMILY_ID));
		when(mockOpenEIntegration.getBillingRecord(FAMILY_ID)).thenReturn(billingRecordWrapper);
		doNothing().when(mockDecorator).decorate(any(BillingRecordWrapper.class));
		doNothing().when(mockBillingPreprocessorIntegration).createBillingRecord(any());

		//Act
		collectorService.triggerBetweenDates(startDate, endDate);

		//Assert
		verify(mockOpenEIntegration).getSupportedFamilyIds();
		verify(mockOpenEIntegration).getFlowInstanceIds(FAMILY_ID, startDate.toString(), endDate.toString());
		verify(mockOpenEIntegration).getBillingRecord(FAMILY_ID);
		verify(mockDecorator).decorate(billingRecordWrapper);
		verify(mockBillingPreprocessorIntegration).createBillingRecord(billingRecordWrapper.getBillingRecord());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDecorator, mockBillingPreprocessorIntegration);
	}

	@Test
	void testShouldNotDecorateWhenNoDecoratorPresent() {
		//Arrange
		var billingRecordWrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		billingRecordWrapper.setFamilyId("not_found");	//"Create" a familyId that has no decorator
		when(mockOpenEIntegration.getBillingRecord(FAMILY_ID)).thenReturn(billingRecordWrapper);
		doNothing().when(mockBillingPreprocessorIntegration).createBillingRecord(any());

		//Act
		collectorService.trigger(FAMILY_ID);

		//Assert
		verify(mockOpenEIntegration).getBillingRecord(FAMILY_ID);
		verify(mockDecorator, times(0)).decorate(billingRecordWrapper);
		verify(mockBillingPreprocessorIntegration).createBillingRecord(billingRecordWrapper.getBillingRecord());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDecorator, mockBillingPreprocessorIntegration);
	}
}
