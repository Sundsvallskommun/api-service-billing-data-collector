package se.sundsvall.billingdatacollector.integration.db;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;

public interface ScheduledBillingRepository extends JpaRepository<ScheduledBillingEntity, String> {

	boolean existsByMunicipalityIdAndExternalIdAndSource(String municipalityId, String externalId, BillingSource source);

	Optional<ScheduledBillingEntity> findByMunicipalityIdAndExternalIdAndSource(String municipalityId, String externalId, BillingSource source);

	List<ScheduledBillingEntity> findAllByPausedFalseAndNextScheduledBillingLessThanEqual(LocalDate date);

	Page<ScheduledBillingEntity> findAllByMunicipalityId(String municipalityId, Pageable pageable);

	Optional<ScheduledBillingEntity> findByMunicipalityIdAndId(String municipalityId, String id);
}
