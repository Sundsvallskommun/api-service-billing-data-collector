package se.sundsvall.billingdatacollector.integration.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.jdbc.Sql;

import se.sundsvall.billingdatacollector.support.annotation.UnitTest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@UnitTest
@Sql(scripts = {
	"/db/truncate.sql",
	"/db/testdata.sql"
})
class FalloutRepositoryTest {

	@Autowired
	private FalloutRepository falloutRepository;

	@ParameterizedTest
	@MethodSource("testValues")
	void testExistsByFamilyIdAndFlowInstanceId(String familyId, String flowInstanceId, boolean expected) {
		var exists = falloutRepository.existsByFamilyIdAndFlowInstanceId(familyId, flowInstanceId);
		assertThat(exists).isEqualTo(expected);
	}

	private static Stream<Arguments> testValues() {
		// Arguments.of(familyId, flowInstanceIds, expected)
		return Stream.of(
			Arguments.of("358", "185376", true),    // Both familyId and flowInstanceId exists
			Arguments.of("358", "123456", false),   // Only familyId exists
			Arguments.of("012", "185376", false)    // Only flowInstanceId exists
		);
	}
}
