package se.sundsvall.billingdatacollector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.zalando.problem.Status;
import org.zalando.problem.ThrowableProblem;

import se.sundsvall.billingdatacollector.TestDataFactory;
import se.sundsvall.billingdatacollector.integration.billingpreprocessor.BillingPreprocessorClient;
import se.sundsvall.billingdatacollector.integration.opene.OpenEIntegration;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.billingdatacollector.service.decorator.BillingRecordDecorator;

import wiremock.com.google.common.collect.Sets;

@ExtendWith(MockitoExtension.class)
class CollectorServiceTest {

	@Mock
	private OpenEIntegration mockOpenEIntegration;

	@Mock
	private BillingPreprocessorClient mockBillingPreprocessorClient;

	@Mock
	private BillingRecordDecorator mockDecorator;

	@Mock
	private DbService mockDbService;

	private static final String SUPPORTED_FAMILY_ID = "123";
	private static final String SUPPORTED_FAMILY_ID_2 = "234";
	private static final Set<String> WANTED_FAMILY_IDS = new HashSet<>(Arrays.asList("123", "234", "345"));
	private static final Set<String> SUPPORTED_FAMILY_IDS = new HashSet<>(Arrays.asList("123", "234"));
	private static final List<String> FLOW_INSTANCE_IDS = List.of("456", "789");

	private static final LocalDate START_DATE = LocalDate.now();
	private static final LocalDate END_DATE = START_DATE.plusDays(1);

	private CollectorService collectorService;

	@BeforeEach
	void setUp() {
		when(mockDecorator.getSupportedFamilyId()).thenReturn(SUPPORTED_FAMILY_ID);
		collectorService = new CollectorService(mockDbService, mockOpenEIntegration, mockBillingPreprocessorClient, List.of(mockDecorator));
	}

	@Test
	void testTriggerBilling() {
		//Arrange
		var billingRecordWrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		when(mockOpenEIntegration.getBillingRecord(SUPPORTED_FAMILY_ID)).thenReturn(Optional.of(billingRecordWrapper));
		doNothing().when(mockDecorator).decorate(any(BillingRecordWrapper.class));
		when(mockBillingPreprocessorClient.createBillingRecord(any())).thenReturn(ResponseEntity.ok().build());
		doNothing().when(mockDbService).saveToHistory(any(BillingRecordWrapper.class), ArgumentMatchers.any());

		//Act
		collectorService.triggerBilling(SUPPORTED_FAMILY_ID);

		//Assert
		verify(mockOpenEIntegration).getBillingRecord(SUPPORTED_FAMILY_ID);
		verify(mockDecorator).decorate(billingRecordWrapper);
		verify(mockDbService).saveToHistory(billingRecordWrapper, ResponseEntity.ok().build());
		verify(mockBillingPreprocessorClient).createBillingRecord(billingRecordWrapper.getBillingRecord());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDecorator, mockBillingPreprocessorClient);
	}

	@Test
	void testTriggerBilling_shouldSaveToFallout_whenPreprocessorException() {
		// Arrange
		when(mockOpenEIntegration.getBillingRecord(SUPPORTED_FAMILY_ID)).thenReturn(Optional.of(TestDataFactory.createKundfakturaBillingRecordWrapper(true)));
		when(mockBillingPreprocessorClient.createBillingRecord(any())).thenThrow(new RuntimeException("Something went wrong"));
		doNothing().when(mockDecorator).decorate(any(BillingRecordWrapper.class));

		// Act
		collectorService.triggerBilling(SUPPORTED_FAMILY_ID);

		// Assert
		verify(mockOpenEIntegration).getBillingRecord(SUPPORTED_FAMILY_ID);
		verify(mockBillingPreprocessorClient).createBillingRecord(any());
		verify(mockDbService, times(0)).saveToHistory(any(BillingRecordWrapper.class), any());
		verify(mockDbService).saveFailedBillingRecord(any(BillingRecordWrapper.class), anyString());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDbService, mockDecorator, mockBillingPreprocessorClient);
	}

	@Test
	void testTriggerBillingBetweenDates_emptyFamilyIds_shouldTriggerBillingAllSupportedFamilyIds() {
		//Arrange
		var billingRecordWrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		when(mockOpenEIntegration.getSupportedFamilyIds()).thenReturn(SUPPORTED_FAMILY_IDS);
		when(mockOpenEIntegration.getFlowInstanceIds(SUPPORTED_FAMILY_ID, START_DATE.toString(), END_DATE.toString())).thenReturn(FLOW_INSTANCE_IDS);
		when(mockOpenEIntegration.getFlowInstanceIds(SUPPORTED_FAMILY_ID_2, START_DATE.toString(), END_DATE.toString())).thenReturn(FLOW_INSTANCE_IDS);
		when(mockOpenEIntegration.getBillingRecord(anyString())).thenReturn(Optional.of(billingRecordWrapper));
		when(mockDbService.hasAlreadyBeenProcessed(anyString(), anyString())).thenReturn(false);
		doNothing().when(mockDbService).saveToHistory(any(BillingRecordWrapper.class), ArgumentMatchers.any());
		doNothing().when(mockDecorator).decorate(any(BillingRecordWrapper.class));
		when(mockBillingPreprocessorClient.createBillingRecord(any())).thenReturn(ResponseEntity.ok().build());

		//Act
		collectorService.triggerBillingBetweenDates(START_DATE, END_DATE, Set.of());

		//Assert
		verify(mockOpenEIntegration).getSupportedFamilyIds();
		verify(mockOpenEIntegration, times(1)).getFlowInstanceIds(SUPPORTED_FAMILY_ID, START_DATE.toString(), END_DATE.toString());
		verify(mockOpenEIntegration, times(2)).getBillingRecord(FLOW_INSTANCE_IDS.getFirst());
		verify(mockOpenEIntegration, times(2)).getBillingRecord(FLOW_INSTANCE_IDS.getLast());
		verify(mockDbService, times(4)).hasAlreadyBeenProcessed(anyString(), anyString());
		verify(mockDbService, times(4)).saveToHistory(billingRecordWrapper, ResponseEntity.ok().build());
		verify(mockDecorator, times(4)).decorate(billingRecordWrapper);
		verify(mockBillingPreprocessorClient, times(4)).createBillingRecord(billingRecordWrapper.getBillingRecord());
		verifyNoMoreInteractions(mockDbService, mockOpenEIntegration, mockDecorator, mockBillingPreprocessorClient);
	}

	@Test
	void testTriggerBillingBetweenDates_withWantedFamilyIds_shouldOnlyTriggerBillingSupported() {
		//Arrange
		var billingRecordWrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		when(mockOpenEIntegration.getSupportedFamilyIds()).thenReturn(Sets.newHashSet(SUPPORTED_FAMILY_IDS));
		when(mockOpenEIntegration.getFlowInstanceIds(SUPPORTED_FAMILY_ID, START_DATE.toString(), END_DATE.toString())).thenReturn(FLOW_INSTANCE_IDS);
		when(mockOpenEIntegration.getFlowInstanceIds(SUPPORTED_FAMILY_ID_2, START_DATE.toString(), END_DATE.toString())).thenReturn(FLOW_INSTANCE_IDS);
		when(mockOpenEIntegration.getBillingRecord(anyString())).thenReturn(Optional.of(billingRecordWrapper));
		when(mockDbService.hasAlreadyBeenProcessed(anyString(), anyString())).thenReturn(false);
		doNothing().when(mockDbService).saveToHistory(any(BillingRecordWrapper.class), ArgumentMatchers.any());
		doNothing().when(mockDecorator).decorate(any(BillingRecordWrapper.class));
		when(mockBillingPreprocessorClient.createBillingRecord(any())).thenReturn(ResponseEntity.ok().build());

		//Act
		collectorService.triggerBillingBetweenDates(START_DATE, END_DATE, WANTED_FAMILY_IDS);

		//Assert
		verify(mockOpenEIntegration).getSupportedFamilyIds();
		verify(mockOpenEIntegration).getFlowInstanceIds(SUPPORTED_FAMILY_ID, START_DATE.toString(), END_DATE.toString());
		verify(mockOpenEIntegration, times(2)).getBillingRecord(FLOW_INSTANCE_IDS.getFirst());
		verify(mockOpenEIntegration, times(2)).getBillingRecord(FLOW_INSTANCE_IDS.getLast());
		verify(mockDbService, times(4)).hasAlreadyBeenProcessed(anyString(), anyString());
		verify(mockDbService, times(4)).saveToHistory(billingRecordWrapper, ResponseEntity.ok().build());
		verify(mockDecorator, times(4)).decorate(billingRecordWrapper);
		verify(mockBillingPreprocessorClient, times(4)).createBillingRecord(billingRecordWrapper.getBillingRecord());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDbService, mockDecorator, mockBillingPreprocessorClient);
	}

	@Test
	void testShouldNotDecorateWhenNoDecoratorPresent() {
		//Arrange
		var billingRecordWrapper = TestDataFactory.createKundfakturaBillingRecordWrapper(true);
		billingRecordWrapper.setFamilyId("not_found");	//"Create" a familyId that has no decorator
		when(mockOpenEIntegration.getBillingRecord(SUPPORTED_FAMILY_ID)).thenReturn(Optional.of(billingRecordWrapper));
		when(mockBillingPreprocessorClient.createBillingRecord(any())).thenReturn(ResponseEntity.ok().build());
		doNothing().when(mockDbService).saveToHistory(any(BillingRecordWrapper.class), ArgumentMatchers.any());

		//Act
		collectorService.triggerBilling(SUPPORTED_FAMILY_ID);

		//Assert
		verify(mockOpenEIntegration).getBillingRecord(SUPPORTED_FAMILY_ID);
		verify(mockDecorator, times(0)).decorate(billingRecordWrapper);
		verify(mockBillingPreprocessorClient).createBillingRecord(billingRecordWrapper.getBillingRecord());
		verify(mockDbService).saveToHistory(billingRecordWrapper, ResponseEntity.ok().build());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDbService, mockDecorator, mockBillingPreprocessorClient);
	}

	@Test
	void testTriggerBillingBetweenDates_noResponseFromOpenE_shouldNotCallDecorateOrCreateBillingRecord() {
		//Arrange
		when(mockOpenEIntegration.getSupportedFamilyIds()).thenReturn(new HashSet<>(List.of(SUPPORTED_FAMILY_ID)));
		when(mockOpenEIntegration.getFlowInstanceIds(SUPPORTED_FAMILY_ID, START_DATE.toString(), END_DATE.toString())).thenReturn(List.of());

		//Act
		collectorService.triggerBillingBetweenDates(START_DATE, END_DATE, WANTED_FAMILY_IDS);

		//Assert
		verify(mockOpenEIntegration).getSupportedFamilyIds();
		verify(mockOpenEIntegration).getFlowInstanceIds(SUPPORTED_FAMILY_ID, START_DATE.toString(), END_DATE.toString());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDbService, mockDecorator, mockBillingPreprocessorClient);
	}

	@Test
	void testTriggerBillingBetweenDates_noSupportedFamilyIds_shouldThrowException() {
		//Arrange
		when(mockOpenEIntegration.getSupportedFamilyIds()).thenReturn(Set.of("something_else"));

		//Act & Assert
		assertThatExceptionOfType(ThrowableProblem.class)
			.isThrownBy(() -> collectorService.triggerBillingBetweenDates(START_DATE, END_DATE, WANTED_FAMILY_IDS))
			.satisfies(throwableProblem -> {
				assertThat(throwableProblem.getStatus()).isEqualTo(Status.BAD_REQUEST);
				assertThat(throwableProblem.getDetail()).contains("Supported familyIds: [something_else]");
				assertThat(throwableProblem.getTitle()).isEqualTo("No supported familyIds found");
			});

		verify(mockOpenEIntegration).getSupportedFamilyIds();
		verifyNoMoreInteractions(mockOpenEIntegration,mockDbService, mockDecorator, mockBillingPreprocessorClient);
	}

	@Test
	void testTriggerBillingBetweenDates_alreadyProcessed_shouldNotTriggerBilling() {
		// Arrange
		when(mockOpenEIntegration.getSupportedFamilyIds()).thenReturn(SUPPORTED_FAMILY_IDS);
		when(mockOpenEIntegration.getFlowInstanceIds(SUPPORTED_FAMILY_ID, START_DATE.toString(), END_DATE.toString())).thenReturn(FLOW_INSTANCE_IDS);
		when(mockOpenEIntegration.getFlowInstanceIds(SUPPORTED_FAMILY_ID_2, START_DATE.toString(), END_DATE.toString())).thenReturn(FLOW_INSTANCE_IDS);
		when(mockDbService.hasAlreadyBeenProcessed(anyString(), anyString())).thenReturn(true);

		// Act
		collectorService.triggerBillingBetweenDates(START_DATE, END_DATE, WANTED_FAMILY_IDS);

		// Assert
		verify(mockOpenEIntegration).getSupportedFamilyIds();
		verify(mockOpenEIntegration, times(1)).getFlowInstanceIds(SUPPORTED_FAMILY_ID, START_DATE.toString(), END_DATE.toString());
		verify(mockDbService, times(4)).hasAlreadyBeenProcessed(anyString(), anyString());
		verify(mockDbService, times(0)).saveToHistory(any(BillingRecordWrapper.class), ArgumentMatchers.any());
		verifyNoMoreInteractions(mockOpenEIntegration, mockDbService, mockDecorator, mockBillingPreprocessorClient);
	}
}
