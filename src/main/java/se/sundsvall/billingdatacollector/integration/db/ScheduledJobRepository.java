package se.sundsvall.billingdatacollector.integration.db;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.billingdatacollector.integration.db.model.ScheduledJobEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "scheduledJobRepository")
public interface ScheduledJobRepository extends JpaRepository<ScheduledJobEntity, String> {
	Optional<ScheduledJobEntity> findFirstByOrderByFetchedEndDateDesc();
}
