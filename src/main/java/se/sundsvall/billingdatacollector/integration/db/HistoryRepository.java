package se.sundsvall.billingdatacollector.integration.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.billingdatacollector.integration.db.model.HistoryEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "historyRepository")
public interface HistoryRepository extends JpaRepository<HistoryEntity, String> {
	/**
	 * Check if a history entity exists by family id and flow instance id
	 * @param familyId the family id
	 * @param flowInstanceId the flow instance id
	 * @return true if the history entity exists, false otherwise
	 */
	boolean existsByFamilyIdAndFlowInstanceId(String familyId, String flowInstanceId);

	/**
	 * Find all history entities by flow instance ids
	 * @param flowInstanceIds the flow instance ids
	 * @return List of history entities
	 */
	List<HistoryEntity> findAllByFlowInstanceIdIn(List<String> flowInstanceIds);
}
