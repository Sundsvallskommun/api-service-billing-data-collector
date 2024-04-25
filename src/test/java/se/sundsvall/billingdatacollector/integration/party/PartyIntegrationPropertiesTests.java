package se.sundsvall.billingdatacollector.integration.party;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.support.annotation.UnitTest;

@UnitTest
@SpringBootTest(classes = Application.class)
class PartyIntegrationPropertiesTests {

    @Autowired
    private PartyIntegrationProperties properties;

    @Test
    void testProperties() {
        assertThat(properties.baseUrl()).isEqualTo("http://party.nosuchhost.com");
        assertThat(properties.oauth2()).isNotNull().satisfies(oauth2 -> {
            assertThat(oauth2.tokenUrl()).isEqualTo("http://token.nosuchhost.com");
            assertThat(oauth2.clientId()).isEqualTo("someClientId");
            assertThat(oauth2.clientSecret()).isEqualTo("someClientSecret");
            assertThat(oauth2.authorizationGrantType()).isEqualTo("client_credentials");
        });
        assertThat(properties.connectTimeout()).isEqualTo(56);
        assertThat(properties.readTimeout()).isEqualTo(78);
    }
}
