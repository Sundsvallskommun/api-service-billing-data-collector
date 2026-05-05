package se.sundsvall.billingdatacollector.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.api.model.InvoicedIn;
import se.sundsvall.billingdatacollector.api.model.ScheduledBilling;
import se.sundsvall.billingdatacollector.integration.db.ScheduledBillingRepository;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;
import se.sundsvall.dept44.problem.Problem;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;
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

	/**
	 * Updates the cadence ({@code billingDaysOfMonth}, {@code billingMonths})
	 * and the {@code paused} flag on an existing scheduled billing.
	 *
	 * <p>
	 * {@code nextScheduledBilling} is <em>preserved</em> — admin updates
	 * to cadence are rare and we don't want them to silently reset billing
	 * progression. If the cadence change makes the parked slot invalid,
	 * delete and re-create the schedule instead.
	 */
	public ScheduledBilling update(String municipalityId, String id, ScheduledBilling scheduledBilling) {
		LOG.info("Updating scheduled billing with id: {} for municipalityId: {}",
			sanitizeForLogging(id), sanitizeForLogging(municipalityId));

		ScheduledBillingEntity existing = repository.findByMunicipalityIdAndId(municipalityId, id)
			.orElseThrow(() -> Problem.builder()
				.withStatus(NOT_FOUND)
				.withTitle(ERROR_SCHEDULED_BILLING_NOT_FOUND)
				.withDetail(DETAIL_SCHEDULED_BILLING_NOT_FOUND_BY_ID + id)
				.build());

		existing.setBillingDaysOfMonth(scheduledBilling.getBillingDaysOfMonth());
		existing.setBillingMonths(scheduledBilling.getBillingMonths());
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

	/**
	 * Creates the scheduled-billing row if it doesn't exist, otherwise updates
	 * its cadence ({@code billingMonths}, {@code billingDaysOfMonth}) and
	 * direction ({@code invoicedIn}).
	 *
	 * <p>
	 * {@code nextScheduledBilling} is set in two cases:
	 * <ol>
	 * <li>when the row is created (using the supplier), or</li>
	 * <li>when an existing row's {@code billingMonths} or {@code invoicedIn}
	 * actually changed — the previously stored progression is then on
	 * a slot that no longer matches the contract and must be recomputed.
	 * A {@code WARN} is logged so operators can manually issue any
	 * credit/additional invoices needed to cover the overlap or gap.</li>
	 * </ol>
	 *
	 * <p>
	 * Otherwise the existing {@code nextScheduledBilling} is preserved so
	 * an unrelated contract update does not reset billing progression.
	 *
	 * <p>
	 * The supplier is lazy so callers can compute an expensive initial
	 * date (e.g. ARREARS skip-one-slot logic) without paying for it on every
	 * UPDATED event.
	 *
	 * @param invoicedIn                  contract's current direction
	 *                                    (ADVANCE / ARREARS); persisted on
	 *                                    the entity to detect future switches
	 * @param initialNextScheduledBilling supplier evaluated when a new row is
	 *                                    created, or when {@code billingMonths}
	 *                                    or {@code invoicedIn} changed for an
	 *                                    existing row
	 */
	public void upsert(String municipalityId, String externalId, BillingSource source,
		Set<Integer> billingMonths, Set<Integer> billingDaysOfMonth, InvoicedIn invoicedIn,
		Supplier<LocalDate> initialNextScheduledBilling) {
		LOG.info("Upserting scheduled billing for municipalityId: {} externalId: {}",
			sanitizeForLogging(municipalityId), sanitizeForLogging(externalId));

		repository.findByMunicipalityIdAndExternalIdAndSource(municipalityId, externalId, source)
			.ifPresentOrElse(
				existing -> {
					var cadenceChanged = !billingMonths.equals(existing.getBillingMonths());
					// existing.invoicedIn == null on rows created before V1_5
					// — treat as "unknown", do not react to a perceived change.
					var directionChanged = existing.getInvoicedIn() != null
						&& !existing.getInvoicedIn().equals(invoicedIn);

					existing.setBillingMonths(billingMonths);
					existing.setBillingDaysOfMonth(billingDaysOfMonth);
					existing.setInvoicedIn(invoicedIn);

					if (cadenceChanged || directionChanged) {
						// Slot the entity is parked on no longer matches the
						// contract — recompute. Operator is responsible for
						// any double or missed billing around the switch
						// (manual credit / manual invoice).
						LOG.warn("Recomputing nextScheduledBilling for externalId {} due to {}",
							sanitizeForLogging(externalId),
							cadenceChanged ? "cadence change" : "invoicedIn switch");
						existing.setNextScheduledBilling(initialNextScheduledBilling.get());
					}
					repository.saveAndFlush(existing);
				},
				() -> repository.saveAndFlush(ScheduledBillingEntity.builder()
					.withMunicipalityId(municipalityId)
					.withExternalId(externalId)
					.withSource(source)
					.withBillingMonths(billingMonths)
					.withBillingDaysOfMonth(billingDaysOfMonth)
					.withInvoicedIn(invoicedIn)
					.withNextScheduledBilling(initialNextScheduledBilling.get())
					.build()));
	}

	public Optional<LocalDate> getNextScheduledBilling(String municipalityId, String externalId, BillingSource source) {
		return repository.findByMunicipalityIdAndExternalIdAndSource(municipalityId, externalId, source)
			.map(ScheduledBillingEntity::getNextScheduledBilling);
	}

	public void deleteScheduledBillingEntity(ScheduledBillingEntity entity) {
		LOG.info("Deleting final scheduled billing for municipalityId: {} externalId: {}",
			sanitizeForLogging(entity.getMunicipalityId()), sanitizeForLogging(entity.getExternalId()));
		repository.delete(entity);
	}

	/**
	 * Persists changes the caller made to {@code entity} (e.g. the scheduler
	 * setting {@code nextScheduledBilling} and {@code lastBilled} after a
	 * successful billing).
	 */
	public void saveScheduledBillingEntity(ScheduledBillingEntity entity) {
		repository.saveAndFlush(entity);
	}

	public void deleteByExternalId(String municipalityId, String externalId, BillingSource source) {
		LOG.info("Deleting scheduled billing for municipalityId: {} externalId: {}",
			sanitizeForLogging(municipalityId), sanitizeForLogging(externalId));

		repository.findByMunicipalityIdAndExternalIdAndSource(municipalityId, externalId, source)
			.ifPresent(repository::delete);
	}

	public List<ScheduledBillingEntity> getDueScheduledBillings() {
		return repository.findAllByPausedFalseAndNextScheduledBillingLessThanEqual(LocalDate.now());
	}
}
