package se.sundsvall.billingdatacollector.integration.db;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.integration.db.model.ScheduledBillingEntity;

public interface ScheduledBillingRepository extends JpaRepository<ScheduledBillingEntity, String> {

	boolean existsByExternalIdAndMunicipalityIdAndSource(String externalId, String municipalityId, BillingSource source);

	Optional<ScheduledBillingEntity> findByMunicipalityIdAndExternalIdAndSource(String municipalityId, String externalId, BillingSource source);

	List<ScheduledBillingEntity> findAllByPausedFalseAndNextScheduledBillingLessThanEqual(LocalDate date);
}
