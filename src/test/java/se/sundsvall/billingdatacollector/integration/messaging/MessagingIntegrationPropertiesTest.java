package se.sundsvall.billingdatacollector.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.billingdatacollector.Application;

@ActiveProfiles("junit")
@SpringBootTest(classes = Application.class)
class MessagingIntegrationPropertiesTest {

	@Autowired
	private MessagingIntegrationProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.baseUrl()).isEqualTo("http://messaging.nosuchhost.com");
		assertThat(properties.oauth2()).isNotNull().satisfies(oauth2 -> {
			assertThat(oauth2.tokenUrl()).isEqualTo("http://token.nosuchhost.com");
			assertThat(oauth2.clientId()).isEqualTo("someClientId");
			assertThat(oauth2.clientSecret()).isEqualTo("someClientSecret");
			assertThat(oauth2.authorizationGrantType()).isEqualTo("client_credentials");
		});
		assertThat(properties.connectTimeout()).isEqualTo(54);
		assertThat(properties.readTimeout()).isEqualTo(32);
	}
}
