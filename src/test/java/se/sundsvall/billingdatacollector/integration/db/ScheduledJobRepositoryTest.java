package se.sundsvall.billingdatacollector.integration.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.Test;
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
class ScheduledJobRepositoryTest {

	@Autowired
	private ScheduledJobRepository scheduledJobRepository;

	@Test
	void testFindFirstByOrderByFetchedEndDateDesc() {
		var scheduledJobEntity = scheduledJobRepository.findFirstByOrderByFetchedEndDateDesc();
		assertThat(scheduledJobEntity).isPresent();
		assertThat(scheduledJobEntity.get().getFetchedStartDate()).isEqualTo(LocalDate.of(2024, 6, 25));
		assertThat(scheduledJobEntity.get().getFetchedEndDate()).isEqualTo(LocalDate.of(2024, 6, 25));
		assertThat(scheduledJobEntity.get().getId()).isEqualTo("ff298e4b-f14f-4494-8f3a-9da8b00207bb");
		assertThat(scheduledJobEntity.get().getProcessed()).isEqualTo(OffsetDateTime.parse("2024-06-26T10:19:17.025118+02:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME));
	}
}
