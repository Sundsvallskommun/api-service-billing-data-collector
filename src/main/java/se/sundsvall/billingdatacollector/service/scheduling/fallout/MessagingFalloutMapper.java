package se.sundsvall.billingdatacollector.service.scheduling.fallout;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import se.sundsvall.billingdatacollector.integration.messaging.FalloutMessageProperties;
import se.sundsvall.billingdatacollector.model.Fallout;

import generated.se.sundsvall.messaging.EmailBatchRequest;
import generated.se.sundsvall.messaging.EmailSender;
import generated.se.sundsvall.messaging.Party;

@Component
public class MessagingFalloutMapper {

	private final FalloutMessageProperties properties;
	private final String applicationName;
	private final String environment;

	public MessagingFalloutMapper(
		@Value("${spring.application.name}") String applicationName,
		@Value("${spring.profiles.active:}") String environment,
		FalloutMessageProperties properties) {
		this.properties = properties;
		this.applicationName = applicationName;
		this.environment = environment;
	}

	public EmailBatchRequest createEmailBatchRequest(List<Fallout> fallouts) {
		//Sort the list by familyId and then flowInstanceId
		fallouts.sort(Comparator.comparing(Fallout::familyId).thenComparing(Fallout::flowInstanceId));

		var template = properties.falloutMailTemplate();

		// Start
		var bodyBuilder = new StringBuilder(template.htmlPrefix());

		//Set the body heading with the date
		bodyBuilder.append(template.bodyPrefix().formatted(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)));

		// Set number of errors and "start" the list.
		bodyBuilder.append(template.listPrefix().formatted(fallouts.size()));

		//Add each fallout with familyId, flowInstanceId and requestId
		fallouts.forEach(fallout ->
			bodyBuilder.append(template.listItem().formatted(fallout.familyId(), fallout.flowInstanceId(), fallout.requestId())));

		//End the list
		bodyBuilder.append(template.listSuffix());

		//Add the sender
		bodyBuilder.append(template.bodySuffix().formatted(properties.sender(), properties.senderName()));

		// The end
		bodyBuilder.append(template.htmlSuffix());

		return createMessagingRequest(bodyBuilder.toString());
	}

	private EmailBatchRequest createMessagingRequest(String message) {
		return new EmailBatchRequest()
			.sender(new EmailSender()
				.name(properties.senderName())
				.address(properties.sender()))
			.subject(properties.falloutMailTemplate().subject().formatted(applicationName, environment))
			.parties(generatePartyList(properties.recipients()))
			.htmlMessage(message);
	}

	private List<Party> generatePartyList(List<String> recipients) {
		return recipients.stream()
			.map(recipient -> new Party().emailAddress(recipient))
			.toList();
	}
}
