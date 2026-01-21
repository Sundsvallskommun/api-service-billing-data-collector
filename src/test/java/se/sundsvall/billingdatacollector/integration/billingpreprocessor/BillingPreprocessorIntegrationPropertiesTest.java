package se.sundsvall.billingdatacollector.integration.billingpreprocessor;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.billingdatacollector.Application;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class BillingPreprocessorIntegrationPropertiesTest {

	@Autowired
	private BillingPreprocessorIntegrationProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.baseUrl()).isEqualTo("http://bpp.nosuchhost.com");
		assertThat(properties.oauth2()).isNotNull().satisfies(oauth2 -> {
			assertThat(oauth2.tokenUrl()).isEqualTo("http://token.nosuchhost.com");
			assertThat(oauth2.clientId()).isEqualTo("someClientId");
			assertThat(oauth2.clientSecret()).isEqualTo("someClientSecret");
			assertThat(oauth2.authorizationGrantType()).isEqualTo("client_credentials");
		});
		assertThat(properties.connectTimeout()).isEqualTo(98);
		assertThat(properties.readTimeout()).isEqualTo(76);
	}
}
