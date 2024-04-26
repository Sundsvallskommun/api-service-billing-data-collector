package se.sundsvall.billingdatacollector.integration.messaging;

import org.springframework.stereotype.Component;

import generated.se.sundsvall.messaging.EmailRequest;
import generated.se.sundsvall.messaging.EmailSender;
import generated.se.sundsvall.messaging.SlackRequest;

@Component
public class MessagingIntegration {

    private final MessagingIntegrationProperties properties;
    private final MessagingClient messagingClient;

    MessagingIntegration(final MessagingIntegrationProperties properties,
            final MessagingClient messagingClient) {
        this.properties = properties;
        this.messagingClient = messagingClient;
    }

    public void sendEmail(final String message) {
        var emailProperties = properties.email();

        var request = new EmailRequest()
            .sender(new EmailSender()
                .name(emailProperties.sender().name())
                .address(emailProperties.sender().emailAddress()))
            .subject(emailProperties.subject())
            .message(message);

        for (var recipient : emailProperties.recipients()) {
            request.setEmailAddress(recipient);

            messagingClient.sendEmail(request);
        }
    }

    public void sendSlackMessage(final String message) {
        var slackProperties = properties.slack();

        var request = new SlackRequest()
            .token(slackProperties.token())
            .message(message);

        for (var channel : slackProperties.channels()) {
            request.setChannel(channel);

            messagingClient.sendSlackMessage(request);
        }
    }
}
