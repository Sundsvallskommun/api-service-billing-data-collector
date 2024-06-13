package se.sundsvall.billingdatacollector.integration.db;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import se.sundsvall.billingdatacollector.integration.db.model.HistoryEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "historyRepository")
public interface HistoryRepository extends JpaRepository<HistoryEntity, String> {
	@Query(value = "SELECT flow_instance_id FROM history WHERE created BETWEEN :start AND :end", nativeQuery = true)
	List<String> findAllByProcessedDateBetween(LocalDate start, LocalDate end);

	boolean existsByFamilyIdAndFlowInstanceId(String familyId, String flowInstanceId);

	List<HistoryEntity> findAllByFlowInstanceIdIn(List<String> flowInstanceIds);
}
