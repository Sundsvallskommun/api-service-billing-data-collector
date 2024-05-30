package se.sundsvall.billingdatacollector.service;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.billingdatacollector.integration.db.FalloutRepository;
import se.sundsvall.billingdatacollector.integration.db.model.FalloutEntity;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import generated.se.sundsvall.billingpreprocessor.BillingRecord;

@ExtendWith(MockitoExtension.class)
class FalloutServiceTest {

	@Mock
	private FalloutRepository mockFalloutRepository;

	@InjectMocks
	private FalloutService falloutService;

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
		falloutService.saveFailedBillingRecord(wrapper, "en error message");

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
		falloutService.saveFailedBillingRecord(wrapper, "an error message");

		//Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository, never()).saveAndFlush(Mockito.any(FalloutEntity.class));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	@Test
	void testSaveFailedOpenEInstance() {
		//Arrange
		byte[] bytes = new byte[0];
		when(mockFalloutRepository.existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID)).thenReturn(false);
		when(mockFalloutRepository.saveAndFlush(Mockito.any(FalloutEntity.class))).thenReturn(FalloutEntity.builder().build());

		//Act
		falloutService.saveFailedOpenEInstance(bytes, FLOW_INSTANCE_ID, FAMILY_ID, "an error message");

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
		falloutService.saveFailedOpenEInstance(bytes, FLOW_INSTANCE_ID, FAMILY_ID, "an error message");

		//Assert
		verify(mockFalloutRepository).existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(FAMILY_ID, FLOW_INSTANCE_ID);
		verify(mockFalloutRepository, never()).saveAndFlush(Mockito.any(FalloutEntity.class));
		verifyNoMoreInteractions(mockFalloutRepository);
	}

	private BillingRecordWrapper createBillingRecordWrapper() {
		return BillingRecordWrapper.builder()
			.withFamilyId(FAMILY_ID)
			.withFlowInstanceId(FLOW_INSTANCE_ID)
			.withBillingRecord(new BillingRecord())
			.build();
	}

}
