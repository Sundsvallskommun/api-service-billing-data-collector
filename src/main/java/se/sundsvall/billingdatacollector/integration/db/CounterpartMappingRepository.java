package se.sundsvall.billingdatacollector.integration.db;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import se.sundsvall.billingdatacollector.integration.db.model.CounterpartMappingEntity;

public interface CounterpartMappingRepository extends JpaRepository<CounterpartMappingEntity, String> {

	/**
	 * Find a counterpart mapping by exact legalId match.
	 *
	 * @param  legalId the legalId to match
	 * @return         the mapping if found
	 */
	Optional<CounterpartMappingEntity> findByLegalId(String legalId);

	/**
	 * Find a counterpart mapping by stakeholder type.
	 *
	 * @param  stakeholderType the stakeholder type to match
	 * @return                 the mapping if found
	 */
	Optional<CounterpartMappingEntity> findByStakeholderType(String stakeholderType);
}
