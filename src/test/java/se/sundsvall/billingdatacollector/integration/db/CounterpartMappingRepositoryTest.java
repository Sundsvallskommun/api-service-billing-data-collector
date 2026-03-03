package se.sundsvall.billingdatacollector.integration.db;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/truncate.sql",
	"/db/testdata.sql"
})
class CounterpartMappingRepositoryTest {

	@Autowired
	private CounterpartMappingRepository repository;

	@Test
	void findByStakeholderType_found() {
		final var stakeholderType = "ASSOCIATION";

		final var result = repository.findByStakeholderType(stakeholderType);

		assertThat(result).isPresent();
		final var entity = result.get();
		assertThat(entity.getId()).isEqualTo("f0882f1d-06bc-47fd-b017-1d8307f5ce97");
		assertThat(entity.getStakeholderType()).isEqualTo(stakeholderType);
		assertThat(entity.getCounterpart()).isEqualTo("456");
		assertThat(entity.getLegalIdPattern()).isNull();
	}

	@Test
	void findByStakeholderType_notFound() {
		final var result = repository.findByStakeholderType("NON_EXISTING_TYPE");

		assertThat(result).isEmpty();
	}
}
