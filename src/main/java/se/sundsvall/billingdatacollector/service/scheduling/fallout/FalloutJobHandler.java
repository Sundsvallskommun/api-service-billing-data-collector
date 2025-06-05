package se.sundsvall.billingdatacollector.service.scheduling.fallout;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.util.CollectionUtils.isEmpty;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import se.sundsvall.billingdatacollector.integration.messaging.FalloutMessageProperties;
import se.sundsvall.billingdatacollector.integration.messaging.MessagingClient;
import se.sundsvall.billingdatacollector.model.Fallout;
import se.sundsvall.billingdatacollector.service.DbService;

@Component
public class FalloutJobHandler {

	private static final Logger LOG = LoggerFactory.getLogger(FalloutJobHandler.class);

	private final MessagingClient messagingClient;
	private final FalloutMessageProperties properties;
	private final MessagingFalloutMapper falloutMapper;
	private final DbService dbService;

	public FalloutJobHandler(
		MessagingClient messagingClient, FalloutMessageProperties properties, MessagingFalloutMapper falloutMapper, DbService dbService) {
		this.messagingClient = messagingClient;
		this.properties = properties;
		this.falloutMapper = falloutMapper;
		this.dbService = dbService;
	}

	@Transactional
	public void handleFallout() {
		if (isBlank(properties.sender()) || isEmpty(properties.recipients())) {
			LOG.warn("Report of billing errors will not be sent as sender or receiver has not been defined in properties.");
			return;
		}

		// Get all unreported fallouts and map them to a Fallout list
		final var unreportedFallouts = dbService.getUnreportedFallouts()
			.stream()
			.map(falloutEntity -> new Fallout(falloutEntity.getFamilyId(), falloutEntity.getFlowInstanceId(), falloutEntity.getMunicipalityId(), falloutEntity.getRequestId()))
			.collect(Collectors.toCollection(ArrayList::new));  // We want a mutable list

		// Send email if there are any unreported fallouts
		if (isEmpty(unreportedFallouts)) {
			LOG.info("No unreported fallouts found.");
		} else {
			LOG.info("Found {} unreported fallouts, will send email", unreportedFallouts.size());
			composeAndSendMail(unreportedFallouts);
			// Mark all fallouts as reported
			dbService.markAllFalloutsAsReported();
		}
	}

	private void composeAndSendMail(List<Fallout> fallouts) {
		final var emailBatchRequest = falloutMapper.createEmailBatchRequest(fallouts);

		messagingClient.sendEmailBatch(fallouts.getFirst().municipalityId(), emailBatchRequest);
	}
}
