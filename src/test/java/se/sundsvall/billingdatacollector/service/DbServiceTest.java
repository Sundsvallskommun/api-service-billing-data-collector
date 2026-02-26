package se.sundsvall.billingdatacollector.service;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
	private static final String MUNICIPALITY_ID = "municipalityId";

	@Test
	void testSaveFailedBillingRecord() {
		// Arrange
		final var wrapper = createBillingRecordWrapper();

		final FalloutEntity expectedEntity = FalloutEntity.builder()
			.withBillingRecordWrapper(wrapper)
			.withFamilyId(FAMILY_ID)
			.withFlowInstanceId(FLOW_INSTANCE_ID)
			.build();

		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(false);
		when(mockFalloutRepository.saveAndFlush(Mockito.any(FalloutEntity.class))).thenReturn(expectedEntity);

		// Act
		dbService.saveFailedBillingRecord(wrapper, "en error message");

		// Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository).saveAndFlush(argThat(entity -> FAMILY_ID.equals(entity.getFamilyId()) &&
			FLOW_INSTANCE_ID.equals(entity.getFlowInstanceId()) &&
			entity.getBillingRecordWrapper().equals(wrapper)));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testSaveFailedBillingRecord_shouldNotSave_whenAlreadyExists() {
		// Arrange
		final var wrapper = createBillingRecordWrapper();
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(true);

		// Act
		dbService.saveFailedBillingRecord(wrapper, "an error message");

		// Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository, never()).saveAndFlush(Mockito.any(FalloutEntity.class));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testSaveFailedFlowInstance() {
		// Arrange
		final byte[] bytes = {};
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(false);
		when(mockFalloutRepository.saveAndFlush(Mockito.any(FalloutEntity.class))).thenReturn(FalloutEntity.builder().build());

		// Act
		dbService.saveFailedFlowInstance(bytes, FLOW_INSTANCE_ID, FAMILY_ID, MUNICIPALITY_ID, "an error message");

		// Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository).saveAndFlush(argThat(entity -> FAMILY_ID.equals(entity.getFamilyId()) &&
			FLOW_INSTANCE_ID.equals(entity.getFlowInstanceId()) &&
			entity.getOpenEInstance().equals(new String(bytes, StandardCharsets.ISO_8859_1))));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testSaveFailedOpenEInstance_shouldNotSave_whenAlreadyExists() {
		// Arrange
		final byte[] bytes = {};
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(true);

		// Act
		dbService.saveFailedFlowInstance(bytes, FLOW_INSTANCE_ID, FAMILY_ID, MUNICIPALITY_ID, "an error message");

		// Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository, never()).saveAndFlush(Mockito.any(FalloutEntity.class));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testSaveToHistory() {
		// Arrange
		final var wrapper = createBillingRecordWrapper();
		final var headers = createHeaders();

		when(mockResponseEntity.getHeaders()).thenReturn(headers);

		// Act
		dbService.saveToHistory(wrapper, mockResponseEntity);

		// Assert
		verify(mockResponseEntity).getHeaders();
		verify(mockHistoryRepository).saveAndFlush(argThat(entity -> FLOW_INSTANCE_ID.equals(entity.getFlowInstanceId()) &&
			FAMILY_ID.equals(entity.getFamilyId()) &&
			"/location/uuid".equals(entity.getLocation())));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@ParameterizedTest
	@MethodSource("processedParameters")
	void testHasAlreadyBeenProcessed(boolean historyExists, boolean falloutExists, boolean expectedOutcome) {
		// Arrange
		when(mockHistoryRepository.existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(historyExists);
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(falloutExists);

		// Act
		final boolean result = dbService.hasAlreadyBeenProcessed(FAMILY_ID, FLOW_INSTANCE_ID);

		// Assert
		assertThat(result).isEqualTo(expectedOutcome);
		verify(mockHistoryRepository).existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceId(FAMILY_ID, FLOW_INSTANCE_ID);
		verifyNoMoreInteractions(mockFalloutRepository);
		verifyNoMoreInteractions(mockHistoryRepository);
	}

	private static Stream<Arguments> processedParameters() {
		// Arguments.of(historyExists, falloutExists, expected), i.e. only false when both are false
		return Stream.of(
			Arguments.of(true, false, true),
			Arguments.of(false, true, true),
			Arguments.of(true, true, true),
			Arguments.of(false, false, false));
	}

	@Test
	void testSaveScheduledJob() {
		// Arrange
		final var startDate = java.time.LocalDate.now();
		final var endDate = java.time.LocalDate.now().plusDays(1);

		// Act
		dbService.saveScheduledJob(startDate, endDate);

		// Assert
		verify(mockScheduledJobRepository).saveAndFlush(argThat(entity -> entity.getFetchedStartDate().equals(startDate) &&
			entity.getFetchedEndDate().equals(endDate)));
		verifyNoMoreInteractions(mockScheduledJobRepository);
	}

	@Test
	void testGetLatestJob() {
		// Arrange
		final var expectedEntity = ScheduledJobEntity.builder().build();
		when(mockScheduledJobRepository.findFirstByOrderByFetchedEndDateDesc()).thenReturn(Optional.of(expectedEntity));

		// Act
		final var result = dbService.getLatestJob();

		// Assert
		assertThat(result)
			.isPresent()
			.contains(expectedEntity);
		verify(mockScheduledJobRepository).findFirstByOrderByFetchedEndDateDesc();
		verifyNoMoreInteractions(mockScheduledJobRepository);
	}

	@Test
	void testGetLatestJob_shouldReturnEmpty_whenNoJobExists() {
		// Arrange
		when(mockScheduledJobRepository.findFirstByOrderByFetchedEndDateDesc()).thenReturn(Optional.empty());

		// Act
		final var result = dbService.getLatestJob();

		// Assert
		assertThat(result).isEmpty();
		verify(mockScheduledJobRepository).findFirstByOrderByFetchedEndDateDesc();
		verifyNoMoreInteractions(mockScheduledJobRepository);
	}

	@Test
	void testGetHistory() {
		// Arrange
		when(mockHistoryRepository.findAllByFlowInstanceIdIn(anyList())).thenReturn(List.of(HistoryEntity.builder().build()));

		// Act
		final var result = dbService.getHistory(List.of("flowInstanceId"));

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
		final var result = dbService.getFallouts(List.of("flowInstanceId"));

		// Assert
		assertThat(result).hasSize(1);
		verify(mockFalloutRepository).findAllByFlowInstanceIdIn(List.of("flowInstanceId"));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testGetUnreportedFallouts() {
		// Arrange
		when(mockFalloutRepository.findAllByReportedIsFalse()).thenReturn(List.of(FalloutEntity.builder().build()));

		// Act
		final var result = dbService.getUnreportedFallouts();

		// Assert
		assertThat(result).hasSize(1);
		verify(mockFalloutRepository).findAllByReportedIsFalse();
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testMarkAllFalloutsAsReported() {
		// Arrange
		final var entities = List.of(FalloutEntity.builder().build());
		when(mockFalloutRepository.findAllByReportedIsFalse()).thenReturn(entities);

		// Act
		dbService.markAllFalloutsAsReported();

		// Assert
		verify(mockFalloutRepository).findAllByReportedIsFalse();
		verify(mockFalloutRepository).saveAllAndFlush(entities);
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	private static @NotNull HttpHeaders createHeaders() {
		final MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("location", "/location/uuid");
		final HttpHeaders headers = new HttpHeaders();
		headers.putAll(map);
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
