package se.sundsvall.billingdatacollector.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.zalando.problem.Problem;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.ScheduledBilling;
import se.sundsvall.billingdatacollector.integration.db.ScheduledBillingRepository;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;

import static org.springframework.transaction.annotation.Propagation.REQUIRES_NEW;
import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;
import static se.sundsvall.billingdatacollector.service.util.ScheduledBillingUtil.calculateNextScheduledBilling;
import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

@Service
@Transactional
public class ScheduledBillingService {

	private static final Logger LOG = LoggerFactory.getLogger(ScheduledBillingService.class);

	private static final String ERROR_SCHEDULED_BILLING_NOT_FOUND = "Scheduled billing not found";
	private static final String DETAIL_SCHEDULED_BILLING_NOT_FOUND_BY_ID = "No scheduled billing found with id: ";

	private final ScheduledBillingRepository repository;

	public ScheduledBillingService(ScheduledBillingRepository repository) {
		this.repository = repository;
	}

	public ScheduledBilling create(String municipalityId, ScheduledBilling scheduledBilling) {
		LOG.info("Creating scheduled billing for municipalityId: {} and externalId: {}",
			sanitizeForLogging(municipalityId), sanitizeForLogging(scheduledBilling.getExternalId()));

		if (repository.existsByMunicipalityIdAndExternalIdAndSource(
			municipalityId,
			scheduledBilling.getExternalId(),
			scheduledBilling.getSource())) {
			throw Problem.builder()
				.withStatus(BAD_REQUEST)
				.withTitle("Duplicate scheduled billing")
				.withDetail("Scheduled billing already exists for externalId: " +
					scheduledBilling.getExternalId() + " and source: " + scheduledBilling.getSource())
				.build();
		}

		LocalDate nextBilling = calculateNextScheduledBilling(
			scheduledBilling.getBillingDaysOfMonth(),
			scheduledBilling.getBillingMonths());

		ScheduledBillingEntity entity = EntityMapper.toScheduledBillingEntity(
			municipalityId, scheduledBilling, nextBilling);

		ScheduledBillingEntity saved = repository.saveAndFlush(entity);

		LOG.info("Created scheduled billing with id: {}", sanitizeForLogging(saved.getId()));

		return EntityMapper.toScheduledBilling(saved);
	}

	public ScheduledBilling update(String municipalityId, String id, ScheduledBilling scheduledBilling) {
		LOG.info("Updating scheduled billing with id: {} for municipalityId: {}",
			sanitizeForLogging(id), sanitizeForLogging(municipalityId));

		ScheduledBillingEntity existing = repository.findByMunicipalityIdAndId(municipalityId, id)
			.orElseThrow(() -> Problem.builder()
				.withStatus(NOT_FOUND)
				.withTitle(ERROR_SCHEDULED_BILLING_NOT_FOUND)
				.withDetail(DETAIL_SCHEDULED_BILLING_NOT_FOUND_BY_ID + id)
				.build());

		LocalDate nextBilling = calculateNextScheduledBilling(
			scheduledBilling.getBillingDaysOfMonth(),
			scheduledBilling.getBillingMonths());

		existing.setBillingDaysOfMonth(scheduledBilling.getBillingDaysOfMonth());
		existing.setBillingMonths(scheduledBilling.getBillingMonths());
		existing.setNextScheduledBilling(nextBilling);
		existing.setPaused(Optional.ofNullable(scheduledBilling.getPaused()).orElse(false));

		ScheduledBillingEntity saved = repository.saveAndFlush(existing);

		return EntityMapper.toScheduledBilling(saved);
	}

	@Transactional(readOnly = true)
	public Page<ScheduledBilling> getAll(String municipalityId, Pageable pageable) {
		LOG.info("Getting all scheduled billings for municipalityId: {}", sanitizeForLogging(municipalityId));

		return repository.findAllByMunicipalityId(municipalityId, pageable)
			.map(EntityMapper::toScheduledBilling);
	}

	@Transactional(readOnly = true)
	public ScheduledBilling getById(String municipalityId, String id) {
		LOG.info("Getting scheduled billing by id: {} for municipalityId: {}",
			sanitizeForLogging(id), sanitizeForLogging(municipalityId));

		return repository.findByMunicipalityIdAndId(municipalityId, id)
			.map(EntityMapper::toScheduledBilling)
			.orElseThrow(() -> Problem.builder()
				.withStatus(NOT_FOUND)
				.withTitle(ERROR_SCHEDULED_BILLING_NOT_FOUND)
				.withDetail(DETAIL_SCHEDULED_BILLING_NOT_FOUND_BY_ID + id)
				.build());
	}

	public void delete(String municipalityId, String id) {
		LOG.info("Deleting scheduled billing with id: {} for municipalityId: {}",
			sanitizeForLogging(id), sanitizeForLogging(municipalityId));

		ScheduledBillingEntity entity = repository.findByMunicipalityIdAndId(municipalityId, id)
			.orElseThrow(() -> Problem.builder()
				.withStatus(NOT_FOUND)
				.withTitle(ERROR_SCHEDULED_BILLING_NOT_FOUND)
				.withDetail(DETAIL_SCHEDULED_BILLING_NOT_FOUND_BY_ID + id)
				.build());

		repository.delete(entity);
	}

	@Transactional(readOnly = true)
	public ScheduledBilling getByExternalId(String municipalityId, BillingSource source, String externalId) {
		LOG.info("Getting scheduled billing by externalId: {}, source: {} for municipalityId: {}",
			sanitizeForLogging(externalId), source, sanitizeForLogging(municipalityId));

		return repository.findByMunicipalityIdAndExternalIdAndSource(municipalityId, externalId, source)
			.map(EntityMapper::toScheduledBilling)
			.orElseThrow(() -> Problem.builder()
				.withStatus(NOT_FOUND)
				.withTitle(ERROR_SCHEDULED_BILLING_NOT_FOUND)
				.withDetail("No scheduled billing found with externalId: " + externalId +
					" and source: " + source)
				.build());
	}

	public List<ScheduledBillingEntity> getDueScheduledBillings() {
		return repository.findAllByPausedFalseAndNextScheduledBillingLessThanEqual(LocalDate.now());
	}

	@Transactional(propagation = REQUIRES_NEW)
	public void updateNextScheduledBilling(ScheduledBillingEntity scheduledBillingEntity) {
		var startFrom = scheduledBillingEntity.getNextScheduledBilling().plusDays(1);
		scheduledBillingEntity.setNextScheduledBilling(calculateNextScheduledBilling(scheduledBillingEntity.getBillingDaysOfMonth(), scheduledBillingEntity.getBillingMonths(), startFrom));
		repository.saveAndFlush(scheduledBillingEntity);
	}
}
