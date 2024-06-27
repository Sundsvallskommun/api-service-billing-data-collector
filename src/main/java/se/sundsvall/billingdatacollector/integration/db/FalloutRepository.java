package se.sundsvall.billingdatacollector.integration.db;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import se.sundsvall.billingdatacollector.integration.db.model.FalloutEntity;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@CircuitBreaker(name = "falloutRepository")
public interface FalloutRepository extends JpaRepository<FalloutEntity, String> {

	/**
	 * Check if a fallout record already exists for the given familyId, flowInstanceId and if there's wrapper present.
	 * We may also want to save a record of a failed OpenE-instance
	 * @param familyId 			the familyId
	 * @param flowInstanceId 	the flowInstanceId
	 * @return true if a BillingRecord fallout record already exists
	 */
	boolean existsByFamilyIdAndFlowInstanceIdAndBillingRecordWrapperIsNotNull(String familyId, String flowInstanceId);

	/**
	 * Check if a fallout record already exists for the given familyId, flowInstanceId and if there's an OpenE-instance present.
	 * @param familyId 			the familyId
	 * @param flowInstanceId 	the flowInstanceId
	 * @return true if an OpenE-instance fallout record already exists
	 */
	boolean existsByFamilyIdAndFlowInstanceIdAndOpenEInstanceIsNotNull(String familyId, String flowInstanceId);

	/**
	 * Find all fallout records for the given flowinstanceIds
	 * @param flowInstanceIds the flowInstanceIds
	 * @return a list of FalloutEntity's
	 */
	List<FalloutEntity> findAllByFlowInstanceIdIn(List<String> flowInstanceIds);

	/**
	 * Check if a fallout record already exists for the given familyId and flowInstanceId
	 * @param familyId the familyId
	 * @param flowInstanceId the flowInstanceId
	 * @return true if a fallout record already exists
	 */
	boolean existsByFamilyIdAndFlowInstanceId(String familyId, String flowInstanceId);

	/**
	 * Find all fallout records that have not been reported
	 * @return a list of unreported FalloutEntity's
	 */
	List<FalloutEntity> findAllByReportedIsFalse();
}
