package se.sundsvall.billingdatacollector.integration.relation;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import se.sundsvall.billingdatacollector.Application;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class)
@ActiveProfiles("junit")
class RelationPropertiesTest {
	@Autowired
	private RelationProperties properties;

	@Test
	void testProperties() {
		assertThat(properties.baseUrl()).isEqualTo("http://relation.nosuchhost.com");
		assertThat(properties.connectTimeout()).isEqualTo(66);
		assertThat(properties.readTimeout()).isEqualTo(67);
		assertThat(properties.oauth2()).isNotNull().satisfies(oauth2 -> {
			assertThat(oauth2.tokenUrl()).isEqualTo("http://token.nosuchhost.com");
			assertThat(oauth2.clientId()).isEqualTo("someClientId");
			assertThat(oauth2.clientSecret()).isEqualTo("someClientSecret");
			assertThat(oauth2.authorizationGrantType()).isEqualTo("client_credentials");
		});
	}
}
