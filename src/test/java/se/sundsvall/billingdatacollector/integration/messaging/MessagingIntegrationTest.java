package se.sundsvall.billingdatacollector.integration.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.MessageResult;
import generated.se.sundsvall.messaging.SlackRequest;

@ExtendWith(MockitoExtension.class)
class MessagingIntegrationTest {

    @Mock
    private MessagingIntegrationProperties mockProperties;

    private MessagingIntegrationProperties.EmailProperties emailProperties;
    private MessagingIntegrationProperties.EmailProperties.Sender emailSender;
    private MessagingIntegrationProperties.SlackProperties slackProperties;

    @Mock
    private MessagingClient mockMessagingClient;

    private MessagingIntegration messagingIntegration;

    @BeforeEach
    void setUp() {
        emailSender = new MessagingIntegrationProperties.EmailProperties.Sender("somename", "someEmailAddress");
        emailProperties = new MessagingIntegrationProperties.EmailProperties("someSubject", List.of("recipientA", "recipientB"), emailSender);
        slackProperties = new MessagingIntegrationProperties.SlackProperties("someToken", List.of("channelA", "channelB"));

        messagingIntegration = new MessagingIntegration(mockProperties, mockMessagingClient);
    }

    @Test
    void sendEmail() {
        var message = "someMessage";

        when(mockProperties.email()).thenReturn(emailProperties);
        when(mockMessagingClient.sendEmail(any(EmailRequest.class)))
            .thenReturn(new MessageResult());

        var requestCaptor = ArgumentCaptor.forClass(EmailRequest.class);

        messagingIntegration.sendEmail(message);

        verify(mockMessagingClient, times(emailProperties.recipients().size())).sendEmail(requestCaptor.capture());
        verify(mockProperties).email();
        verifyNoMoreInteractions(mockMessagingClient, mockProperties);

        for (var actualRequest : requestCaptor.getAllValues()) {
            assertThat(actualRequest.getMessage()).isEqualTo(message);
            assertThat(actualRequest.getSubject()).isEqualTo(emailProperties.subject());
            assertThat(actualRequest.getSender()).isNotNull().satisfies(sender -> {
                assertThat(sender.getName()).isEqualTo(emailSender.name());
                assertThat(sender.getAddress()).isEqualTo(emailSender.emailAddress());
            });
            assertThat(actualRequest.getEmailAddress()).isIn(emailProperties.recipients());
        }
    }

    @Test
    void sendSlackMessage() {
        var message = "someMessage";

        when(mockProperties.slack()).thenReturn(slackProperties);
        when(mockMessagingClient.sendSlackMessage(any(SlackRequest.class)))
            .thenReturn(new MessageResult());

        var requestCaptor = ArgumentCaptor.forClass(SlackRequest.class);

        messagingIntegration.sendSlackMessage(message);

        verify(mockMessagingClient, times(slackProperties.channels().size())).sendSlackMessage(requestCaptor.capture());
        verify(mockProperties).slack();
        verifyNoMoreInteractions(mockMessagingClient, mockProperties);

        for (var actualRequest : requestCaptor.getAllValues()) {
            assertThat(actualRequest.getToken()).isEqualTo(slackProperties.token());
            assertThat(actualRequest.getMessage()).isEqualTo(message);
            assertThat(actualRequest.getChannel()).isIn(slackProperties.channels());
        }
    }
}
