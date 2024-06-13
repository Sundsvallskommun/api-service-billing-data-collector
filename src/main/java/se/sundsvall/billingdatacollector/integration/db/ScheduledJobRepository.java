package se.sundsvall.billingdatacollector.integration.db;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.billingdatacollector.integration.db.model.ScheduledJobEntity;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJobEntity, String> {

	Optional<ScheduledJobEntity> findFirstByOrderByFetchedEndDateDesc();
}
