package se.sundsvall.billingdatacollector.integration.messaging;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE;
import static se.sundsvall.billingdatacollector.integration.messaging.MessagingIntegrationConfiguration.CLIENT_ID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import generated.se.sundsvall.messaging.EmailBatchRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;

@FeignClient(
	name = CLIENT_ID,
	configuration = MessagingIntegrationConfiguration.class,
	url = "${integration.messaging.base-url}")
public interface MessagingClient {

	@PostMapping(
		path = "/{municipalityId}/email/batch",
		consumes = APPLICATION_JSON_VALUE,
		produces = { APPLICATION_JSON_VALUE, APPLICATION_PROBLEM_JSON_VALUE })
	MessageBatchResult sendEmailBatch(@PathVariable("municipalityId") String municipalityId, @RequestBody final EmailBatchRequest request);
}
