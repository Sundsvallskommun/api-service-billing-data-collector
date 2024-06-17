package se.sundsvall.billingdatacollector.service;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import se.sundsvall.billingdatacollector.integration.db.FalloutRepository;
import se.sundsvall.billingdatacollector.integration.db.HistoryRepository;
import se.sundsvall.billingdatacollector.integration.db.ScheduledJobRepository;
import se.sundsvall.billingdatacollector.integration.db.model.FalloutEntity;
import se.sundsvall.billingdatacollector.integration.db.model.HistoryEntity;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledJobEntity;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import jakarta.transaction.Transactional;

@Service
@Transactional
public class DbService {

	private static final Logger LOG = LoggerFactory.getLogger(DbService.class);

	private final FalloutRepository falloutRepository;
	private final HistoryRepository historyRepository;
	private final ScheduledJobRepository scheduledJobRepository;

	public DbService(FalloutRepository falloutRepository, HistoryRepository historyRepository, ScheduledJobRepository scheduledJobRepository) {
		this.falloutRepository = falloutRepository;
		this.historyRepository = historyRepository;
		this.scheduledJobRepository = scheduledJobRepository;
	}

	public void saveFailedBillingRecord(BillingRecordWrapper wrapper, String message) {
		if(!falloutRepository.existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(wrapper.getFamilyId(), wrapper.getFlowInstanceId())) {
			LOG.info("Saving fallout billing record for BPP with familyId: {} and flowInstanceId: {}", wrapper.getFamilyId(), wrapper.getFlowInstanceId());
			falloutRepository.saveAndFlush(EntityMapper.mapToBillingRecordFalloutEntity(wrapper, message));
		} else {
			LOG.info("Fallout billing record already exists for familyId: {} and flowInstanceId: {}", wrapper.getFamilyId(), wrapper.getFlowInstanceId());
		}
	}

	public void saveFailedFlowInstance(byte[] bytes, String flowInstanceId, String familyId, String message) {
		if (!falloutRepository.existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(familyId, flowInstanceId)) {
			LOG.info("Saving fallout billing record for OpenE with familyId: {} and flowInstanceId: {}", familyId, flowInstanceId);
			falloutRepository.saveAndFlush(EntityMapper.mapToOpenEFalloutEntity(bytes, flowInstanceId, familyId, message));
		} else {
			LOG.info("Fallout instance for OpenE already exists for familyId: {} and flowInstanceId: {}", familyId, flowInstanceId);
		}
	}

	public void saveToHistory(BillingRecordWrapper wrapper, ResponseEntity<Void> response) {
		var uri = Optional.of(response.getHeaders())
			.map(HttpHeaders::getLocation)
			.map(URI::toString)
			.orElse(null);

		historyRepository.saveAndFlush(EntityMapper.mapToHistoryEntity(wrapper, uri));

		LOG.info("Saved record to history for familyId: {} and flowInstanceId: {}", wrapper.getFamilyId(), wrapper.getFlowInstanceId());
	}

	public boolean hasAlreadyBeenProcessed(String familyId, String flowInstanceId) {
		var existInHistory = historyRepository.existsByFamilyIdAndFlowInstanceId(familyId, flowInstanceId);
		var existInFallout = falloutRepository.existsByFamilyIdAndFlowInstanceId(familyId, flowInstanceId);

		return existInHistory || existInFallout;
	}

	public void saveScheduledJob(LocalDate startDate, LocalDate endDate) {
		scheduledJobRepository.saveAndFlush(EntityMapper.mapToScheduledJobEntity(startDate, endDate));
	}

	public Optional<ScheduledJobEntity> getLatestJob() {
		return scheduledJobRepository.findFirstByOrderByFetchedEndDateDesc();
	}

	public List<HistoryEntity> getHistory(List<String> flowInstanceIds) {
		return historyRepository.findAllByFlowInstanceIdIn(flowInstanceIds);
	}

	public List<FalloutEntity> getFallouts(List<String> flowInstanceIds) {
		return falloutRepository.findAllByFlowInstanceIdIn(flowInstanceIds);
	}
}
