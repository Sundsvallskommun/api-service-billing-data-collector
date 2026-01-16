package se.sundsvall.billingdatacollector.service;

import static org.zalando.problem.Status.BAD_REQUEST;
import static org.zalando.problem.Status.NOT_FOUND;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
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

@Service
@Transactional
public class ScheduledBillingService {

	private static final Logger LOG = LoggerFactory.getLogger(ScheduledBillingService.class);

	private final ScheduledBillingRepository repository;

	public ScheduledBillingService(ScheduledBillingRepository repository) {
		this.repository = repository;
	}

	public ScheduledBilling create(String municipalityId, ScheduledBilling scheduledBilling) {
		LOG.info("Creating scheduled billing for municipalityId: {} and externalId: {}",
			municipalityId, scheduledBilling.getExternalId());

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

		LOG.info("Created scheduled billing with id: {}", saved.getId());

		return EntityMapper.toScheduledBilling(saved);
	}

	public ScheduledBilling update(String municipalityId, String id, ScheduledBilling scheduledBilling) {
		LOG.info("Updating scheduled billing with id: {} for municipalityId: {}", id, municipalityId);

		ScheduledBillingEntity existing = repository.findByMunicipalityIdAndId(municipalityId, id)
			.orElseThrow(() -> Problem.builder()
				.withStatus(NOT_FOUND)
				.withTitle("Scheduled billing not found")
				.withDetail("No scheduled billing found with id: " + id)
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
		LOG.info("Getting all scheduled billings for municipalityId: {}", municipalityId);

		return repository.findAllByMunicipalityId(municipalityId, pageable)
			.map(EntityMapper::toScheduledBilling);
	}

	@Transactional(readOnly = true)
	public ScheduledBilling getById(String municipalityId, String id) {
		LOG.info("Getting scheduled billing by id: {} for municipalityId: {}", id, municipalityId);

		return repository.findByMunicipalityIdAndId(municipalityId, id)
			.map(EntityMapper::toScheduledBilling)
			.orElseThrow(() -> Problem.builder()
				.withStatus(NOT_FOUND)
				.withTitle("Scheduled billing not found")
				.withDetail("No scheduled billing found with id: " + id)
				.build());
	}

	public void delete(String municipalityId, String id) {
		LOG.info("Deleting scheduled billing with id: {} for municipalityId: {}", id, municipalityId);

		ScheduledBillingEntity entity = repository.findByMunicipalityIdAndId(municipalityId, id)
			.orElseThrow(() -> Problem.builder()
				.withStatus(NOT_FOUND)
				.withTitle("Scheduled billing not found")
				.withDetail("No scheduled billing found with id: " + id)
				.build());

		repository.delete(entity);
	}

	@Transactional(readOnly = true)
	public ScheduledBilling getByExternalId(String municipalityId, BillingSource source, String externalId) {
		LOG.info("Getting scheduled billing by externalId: {}, source: {} for municipalityId: {}",
			externalId, source, municipalityId);

		return repository.findByMunicipalityIdAndExternalIdAndSource(municipalityId, externalId, source)
			.map(EntityMapper::toScheduledBilling)
			.orElseThrow(() -> Problem.builder()
				.withStatus(NOT_FOUND)
				.withTitle("Scheduled billing not found")
				.withDetail("No scheduled billing found with externalId: " + externalId +
					" and source: " + source)
				.build());
	}

	LocalDate calculateNextScheduledBilling(Set<Integer> billingDaysOfMonth, Set<Integer> billingMonths) {
		if (billingDaysOfMonth == null || billingDaysOfMonth.isEmpty()) {
			throw new IllegalArgumentException("billingDaysOfMonth must not be empty");
		}
		if (billingMonths == null || billingMonths.isEmpty()) {
			throw new IllegalArgumentException("billingMonths must not be empty");
		}

		LocalDate today = LocalDate.now();

		for (int monthOffset = 0; monthOffset <= 12; monthOffset++) {
			LocalDate checkMonth = today.plusMonths(monthOffset);
			int month = checkMonth.getMonthValue();

			if (billingMonths.contains(month)) {
				for (Integer day : billingDaysOfMonth.stream().sorted().toList()) {
					int actualDay = Math.min(day, checkMonth.lengthOfMonth());
					LocalDate potentialDate = LocalDate.of(checkMonth.getYear(), month, actualDay);

					if (!potentialDate.isBefore(today)) {
						return potentialDate;
					}
				}
			}
		}

		// This should never be reached since we check 13 months (covers all 12 calendar months)
		throw new IllegalStateException("Unable to calculate next scheduled billing date");
	}
}
