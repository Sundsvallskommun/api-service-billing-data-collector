package se.sundsvall.billingdatacollector.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import se.sundsvall.billingdatacollector.integration.db.FalloutRepository;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import jakarta.transaction.Transactional;

/**
 * Service for saving fallout data.
 * Both methods check if a fallout record already exists for the given familyId, flowInstanceId
 * and if a "payload" is present.
 */
@Service
@Transactional
public class FalloutService {

	private static final Logger LOG = LoggerFactory.getLogger(FalloutService.class);

	private final FalloutRepository falloutRepository;

	public FalloutService(FalloutRepository falloutRepository) {
		this.falloutRepository = falloutRepository;
	}

	public void saveFailedBillingRecord(BillingRecordWrapper wrapper, String message) {
		if(!falloutRepository.existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(wrapper.getFamilyId(), wrapper.getFlowInstanceId())) {
			LOG.info("Saving failed billing record for familyId: {} and flowInstanceId: {}", wrapper.getFamilyId(), wrapper.getFlowInstanceId());
			falloutRepository.saveAndFlush(FalloutEntityMapper.mapToBillingRecordFalloutEntity(wrapper, message));
		} else {
			LOG.info("Fallout billing record already exists for familyId: {} and flowInstanceId: {}", wrapper.getFamilyId(), wrapper.getFlowInstanceId());
		}
	}

	public void saveFailedOpenEInstance(byte[] bytes, String flowInstanceId, String familyId, String message) {
		if (!falloutRepository.existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(familyId, flowInstanceId)) {
			LOG.info("Saving failed OpenE instance for familyId: {} and flowInstanceId: {}", familyId, flowInstanceId);
			falloutRepository.saveAndFlush(FalloutEntityMapper.mapToOpenEFalloutEntity(bytes, flowInstanceId, familyId, message));
		} else {
			LOG.info("Failed OpenE instance already exists for familyId: {} and flowInstanceId: {}", familyId, flowInstanceId);
		}
	}
}
