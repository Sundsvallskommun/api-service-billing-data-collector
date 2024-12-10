package se.sundsvall.billingdatacollector.service.scheduling.fallout;

import static org.assertj.core.api.Assertions.assertThat;

import generated.se.sundsvall.messaging.Party;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.model.Fallout;
import se.sundsvall.billingdatacollector.support.annotation.UnitTest;

@SpringBootTest(classes = Application.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@UnitTest
class MessagingFalloutMapperTest {

	@Autowired
	private MessagingFalloutMapper mapper;

	private static final String HTML_MESSAGE = """
		<!DOCTYPE html>
		      <html lang="en" xmlns="http://www.w3.org/1999/xhtml">
		          <body>
		              <b>Följande problem har inträffat vid generering av faktura-poster (%s)</b>
		              <p>
		                  <i>3 st. fel:</i>
		                  <ul>
		                      <li> familyId: 123, flowInstanceId: 12345, requestId: abc123</li>
		                      <li> familyId: 123, flowInstanceId: 23456, requestId: abc234</li>
		                      <li> familyId: 234, flowInstanceId: 34567, requestId: abc345</li>
		                  </ul>
		                  <p>
		                      <b>Med vänlig hälsning
		                      <br/>
		                      <a href="mailto:dummy@sundsvall.se">Billing Data Collector</a>
		                  </b>
		              </p>
		          </body>
		      </html>""";

	@Test
	void testComposeFalloutEmail() {
		// Arrange
		final var fallout1 = new Fallout("123", "12345", "2281", "abc123");
		final var fallout2 = new Fallout("123", "23456", "2281", "abc234");
		final var fallout3 = new Fallout("234", "34567", "2281", "abc345");

		// Act
		final var emailBatchRequest = mapper.createEmailBatchRequest(new ArrayList<>(List.of(fallout1, fallout2, fallout3)));

		assertThat(emailBatchRequest.getAttachments()).isEmpty();
		assertThat(emailBatchRequest.getHeaders()).isEmpty();
		assertThat(emailBatchRequest.getSender()).satisfies(sender -> {
			assertThat(sender.getName()).isEqualTo("Billing Data Collector");
			assertThat(sender.getAddress()).isEqualTo("dummy@sundsvall.se");
			assertThat(sender.getReplyTo()).isNull();
		});
		assertThat(emailBatchRequest.getSubject()).isEqualTo("Fel vid hämtning/skapande av faktura-poster");
		assertThat(emailBatchRequest.getMessage()).isNull();
		assertThat(emailBatchRequest.getParties()).containsExactlyInAnyOrder(
			new Party().emailAddress("test@nowhere.com"),
			new Party().emailAddress("test2@nowhere.com"));
		// Replace all spaces and compare
		assertThat(
			emailBatchRequest.getHtmlMessage().replaceAll("\\s+", "")).isEqualTo(HTML_MESSAGE.formatted(LocalDate.now()).replaceAll("\\s+", ""));
	}
}
