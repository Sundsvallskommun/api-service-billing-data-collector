package se.sundsvall.billingdatacollector.integration.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("junit")
@Sql(scripts = {
	"/db/truncate.sql",
	"/db/testdata.sql"
})
class HistoryRepositoryTest {

	@Autowired
	private HistoryRepository historyRepository;

	@ParameterizedTest
	@MethodSource("testValues")
	void testExistsByFamilyIdAndFlowInstanceId(String familyId, String flowInstanceId, boolean expected) {
		final var exists = historyRepository.existsByFamilyIdAndFlowInstanceId(familyId, flowInstanceId);
		assertThat(exists).isEqualTo(expected);
	}

	private static Stream<Arguments> testValues() {
		// Arguments.of(familyId, flowInstanceIds, expected)
		return Stream.of(
			Arguments.of("358", "185375", true),    // Both familyId and flowInstanceId exists
			Arguments.of("358", "123456", false),   // Only familyId exists
			Arguments.of("012", "185377", false)    // Only flowInstanceId exists
		);
	}

	@Test
	void testFindAllByFlowInstanceIdIn() {
		// Only verifying that it finds the correct entities, not the content
		final var historyEntities = historyRepository.findAllByFlowInstanceIdIn(List.of("185375", "185377", "doesnt_exist"));
		assertThat(historyEntities).hasSize(2);
		historyEntities.forEach(historyEntity -> assertThat(historyEntity.getFamilyId()).isEqualTo("358"));
		historyEntities.forEach(historyEntity -> assertThat(historyEntity.getFlowInstanceId()).isIn("185375", "185377"));
	}
}
