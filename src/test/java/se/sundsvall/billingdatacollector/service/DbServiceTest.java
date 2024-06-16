package se.sundsvall.billingdatacollector.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import se.sundsvall.billingdatacollector.integration.db.FalloutRepository;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.integration.db.ScheduledJobRepository;
import se.sundsvall.billingdatacollector.integration.db.model.FalloutEntity;
import se.sundsvall.billingdatacollector.integration.db.model.HistoryEntity;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledJobEntity;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;

@ExtendWith(MockitoExtension.class)
class DbServiceTest {

	@Mock
	private FalloutRepository mockFalloutRepository;

	@Mock
	private HistoryRepository mockHistoryRepository;

	@Mock
	private ScheduledJobRepository mockScheduledJobRepository;

	@Mock
	private ResponseEntity<Void> mockResponseEntity;

	@InjectMocks
	private DbService dbService;

	private static final String FAMILY_ID = "familyId";
	private static final String FLOW_INSTANCE_ID = "flowInstanceId";

	@Test
	void testSaveFailedBillingRecord() {
		//Arrange
		var wrapper = createBillingRecordWrapper();

		FalloutEntity expectedEntity = FalloutEntity.builder()
			.withBillingRecordWrapper(wrapper)
			.withFamilyId(FAMILY_ID)
			.withFlowInstanceId(FLOW_INSTANCE_ID)
			.build();

		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(false);
		when(mockFalloutRepository.saveAndFlush(Mockito.any(FalloutEntity.class))).thenReturn(expectedEntity);

		//Act
		dbService.saveFailedBillingRecord(wrapper, "en error message");

		//Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository).saveAndFlush(argThat(entity ->
			entity.getFamilyId().equals(FAMILY_ID) &&
			entity.getFlowInstanceId().equals(FLOW_INSTANCE_ID) &&
			entity.getBillingRecordWrapper().equals(wrapper)));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testSaveFailedBillingRecord_shouldNotSave_whenAlreadyExists() {
		//Arrange
		var wrapper = createBillingRecordWrapper();
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(true);

		//Act
		dbService.saveFailedBillingRecord(wrapper, "an error message");

		//Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository, never()).saveAndFlush(Mockito.any(FalloutEntity.class));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testSaveFailedFlowInstance() {
		//Arrange
		byte[] bytes = new byte[0];
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(false);
		when(mockFalloutRepository.saveAndFlush(Mockito.any(FalloutEntity.class))).thenReturn(FalloutEntity.builder().build());

		//Act
		dbService.saveFailedFlowInstance(bytes, FLOW_INSTANCE_ID, FAMILY_ID, "an error message");

		//Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository).saveAndFlush(argThat(entity ->
			entity.getFamilyId().equals(FAMILY_ID) &&
			entity.getFlowInstanceId().equals(FLOW_INSTANCE_ID) &&
			entity.getOpenEInstance().equals(new String(bytes, StandardCharsets.ISO_8859_1))));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testSaveFailedOpenEInstance_shouldNotSave_whenAlreadyExists() {
		//Arrange
		byte[] bytes = new byte[0];
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(true);

		//Act
		dbService.saveFailedFlowInstance(bytes, FLOW_INSTANCE_ID, FAMILY_ID, "an error message");

		//Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository, never()).saveAndFlush(Mockito.any(FalloutEntity.class));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testSaveToHistory() {
		//Arrange
		var wrapper = createBillingRecordWrapper();
		var headers = createHeaders();

		when(mockResponseEntity.getHeaders()).thenReturn(headers);

		//Act
		dbService.saveToHistory(wrapper, mockResponseEntity);

		//Assert
		verify(mockResponseEntity).getHeaders();
		verify(mockHistoryRepository).saveAndFlush(argThat(entity ->
			entity.getFlowInstanceId().equals(FLOW_INSTANCE_ID) &&
			entity.getFamilyId().equals(FAMILY_ID) &&
			entity.getLocation().equals("/location/uuid")));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testHasAlreadyBeenProcessed_shouldReturnTrue_whenEitherIsTrue() {
		//Arrange
		when(mockHistoryRepository.existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(true);
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(false);

		//Act
		boolean result = dbService.hasAlreadyBeenProcessed(FAMILY_ID, FLOW_INSTANCE_ID);

		//Assert
		assertThat(result).isTrue();
		verify(mockHistoryRepository).existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID);
		verifyNoMoreInteractions(mockFalloutRepository);
		verifyNoMoreInteractions(mockHistoryRepository);
	}

	@Test
	void testHasAlreadyBeenProcessed_shouldReturnFalse_whenNeitherIsTrue() {
		//Arrange
		when(mockHistoryRepository.existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(false);
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(false);

		//Act
		boolean result = dbService.hasAlreadyBeenProcessed(FAMILY_ID, FLOW_INSTANCE_ID);

		//Assert
		assertThat(result).isFalse();
		verify(mockHistoryRepository).existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID);
		verifyNoMoreInteractions(mockFalloutRepository);
		verifyNoMoreInteractions(mockHistoryRepository);
	}

	@Test
	void testSaveScheduledJob() {
		//Arrange
		var startDate = java.time.LocalDate.now();
		var endDate = java.time.LocalDate.now().plusDays(1);

		//Act
		dbService.saveScheduledJob(startDate, endDate);

		//Assert
		verify(mockScheduledJobRepository).saveAndFlush(argThat(entity ->
			entity.getFetchedStartDate().equals(startDate) &&
			entity.getFetchedEndDate().equals(endDate)));
		verifyNoMoreInteractions(mockScheduledJobRepository);
	}

	@Test
	void testGetLatestJob() {
		//Arrange
		var expectedEntity = ScheduledJobEntity.builder().build();
		when(mockScheduledJobRepository.findFirstByOrderByFetchedEndDateDesc()).thenReturn(Optional.of(expectedEntity));

		//Act
		var result = dbService.getLatestJob();

		//Assert
		assertThat(result)
			.isPresent()
			.contains(expectedEntity);
		verify(mockScheduledJobRepository).findFirstByOrderByFetchedEndDateDesc();
		verifyNoMoreInteractions(mockScheduledJobRepository);
	}

	@Test
	void testGetLatestJob_shouldReturnEmpty_whenNoJobExists() {
		//Arrange
		when(mockScheduledJobRepository.findFirstByOrderByFetchedEndDateDesc()).thenReturn(Optional.empty());

		//Act
		var result = dbService.getLatestJob();

		//Assert
		assertThat(result).isEmpty();
		verify(mockScheduledJobRepository).findFirstByOrderByFetchedEndDateDesc();
		verifyNoMoreInteractions(mockScheduledJobRepository);
	}

	@Test
	void testGetHistory() {
		// Arrange
		when(mockHistoryRepository.findAllByFlowInstanceIdIn(anyList())).thenReturn(List.of(HistoryEntity.builder().build()));

		// Act
		var result = dbService.getHistory(List.of("flowInstanceId"));

		// Assert
		assertThat(result).hasSize(1);
		verify(mockHistoryRepository).findAllByFlowInstanceIdIn(List.of("flowInstanceId"));
		verifyNoMoreInteractions(mockHistoryRepository);
	}

	@Test
	void testGetFallouts() {
		// Arrange
		when(mockFalloutRepository.findAllByFlowInstanceIdIn(anyList())).thenReturn(List.of(FalloutEntity.builder().build()));

		// Act
		var result = dbService.getFallouts(List.of("flowInstanceId"));

		// Assert
		assertThat(result).hasSize(1);
		verify(mockFalloutRepository).findAllByFlowInstanceIdIn(List.of("flowInstanceId"));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	private static @NotNull HttpHeaders createHeaders() {
		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("location", "/location/uuid");
		HttpHeaders headers = new HttpHeaders();
		headers.addAll(map);
		return headers;
	}

	private static BillingRecordWrapper createBillingRecordWrapper() {
		return BillingRecordWrapper.builder()
			.withFamilyId(FAMILY_ID)
			.withFlowInstanceId(FLOW_INSTANCE_ID)
			.withBillingRecord(new BillingRecord())
			.build();
	}
}
