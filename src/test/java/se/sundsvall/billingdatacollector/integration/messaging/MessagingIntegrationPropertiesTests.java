package se.sundsvall.billingdatacollector.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import se.sundsvall.billingdatacollector.Application;
import se.sundsvall.billingdatacollector.support.annotation.UnitTest;

@UnitTest
@SpringBootTest(classes = Application.class)
class MessagingIntegrationPropertiesTests {

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
        assertThat(properties.email()).isNotNull().satisfies(emailProperties -> {
            assertThat(emailProperties.sender()).isNotNull().satisfies(sender -> {
                assertThat(sender.name()).isEqualTo("someName");
                assertThat(sender.emailAddress()).isEqualTo("someEmailAddress");
            });
            assertThat(emailProperties.subject()).isEqualTo("someSubject");
            assertThat(emailProperties.recipients()).containsExactlyInAnyOrder("recipientA", "recipientB");
        });
        assertThat(properties.slack()).isNotNull().satisfies(slackProperties -> {
            assertThat(slackProperties.token()).isEqualTo("someToken");
            assertThat(slackProperties.channels()).containsExactlyInAnyOrder("channelA", "channelB", "channelC");
        });
    }
}
