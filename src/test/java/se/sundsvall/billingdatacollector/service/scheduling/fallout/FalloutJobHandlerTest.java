package se.sundsvall.billingdatacollector.service.scheduling.fallout;

import generated.se.sundsvall.messaging.EmailBatchRequest;
import generated.se.sundsvall.messaging.MessageBatchResult;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import se.sundsvall.billingdatacollector.integration.db.model.FalloutEntity;
import se.sundsvall.billingdatacollector.integration.messaging.FalloutMessageProperties;
import se.sundsvall.billingdatacollector.integration.messaging.MessagingClient;
import se.sundsvall.billingdatacollector.service.DbService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FalloutJobHandlerTest {

	@Mock
	private MessagingClient mockMessagingClient;

	@Mock
	private FalloutMessageProperties mockProperties;

	@Mock
	private FalloutMessageProperties.FalloutMailTemplate mockFalloutMailTemplate;

	@Mock
	private MessagingFalloutMapper mockFalloutMapper;

	@Mock
	private DbService mockDbService;

	@InjectMocks
	private FalloutJobHandler jobHandler;

	@Test
	void testHandleFallout_shouldNotSendEmail_ifNoSenderIsPresent() {
		// Arrange
		when(mockProperties.sender()).thenReturn(null);

		// Act
		jobHandler.handleFallout();

		// Assert
		verify(mockProperties).sender();
		verifyNoMoreInteractions(mockProperties);
		verifyNoInteractions(mockMessagingClient, mockFalloutMapper, mockDbService);
	}

	@Test
	void testHandleFallout_shouldNotSendEmail_ifNoRecipientsArePresent() {
		// Arrange
		when(mockProperties.sender()).thenReturn("sender");
		when(mockProperties.recipients()).thenReturn(null);

		// Act
		jobHandler.handleFallout();

		// Assert
		verify(mockProperties).sender();
		verify(mockProperties).recipients();
		verifyNoMoreInteractions(mockProperties);
		verifyNoInteractions(mockMessagingClient, mockFalloutMapper, mockDbService);
	}

	@Test
	void testHandleFallout_shouldNotMarkFalloutsAsReported_whenNoFalloutsWereFound() {
		// Arrange
		when(mockProperties.sender()).thenReturn("sender");
		when(mockProperties.recipients()).thenReturn(List.of("recipient"));
		when(mockDbService.getUnreportedFallouts()).thenReturn(List.of());

		// Act
		jobHandler.handleFallout();

		// Assert
		verify(mockProperties).sender();
		verify(mockProperties).recipients();
		verify(mockDbService).getUnreportedFallouts();
		verifyNoMoreInteractions(mockProperties, mockDbService);
		verifyNoInteractions(mockMessagingClient, mockFalloutMapper);
	}

	@Test
	void testHandleFallout() {
		// Arrange
		when(mockProperties.sender()).thenReturn("sender");
		when(mockProperties.recipients()).thenReturn(List.of("recipient"));
		when(mockDbService.getUnreportedFallouts()).thenReturn(List.of(FalloutEntity.builder().withMunicipalityId("2281").build()));
		when(mockFalloutMapper.createEmailBatchRequest(Mockito.anyList())).thenReturn(new EmailBatchRequest());
		when(mockMessagingClient.sendEmailBatch(any(), any(EmailBatchRequest.class))).thenReturn(new MessageBatchResult());

		// Act
		jobHandler.handleFallout();

		// Assert
		verify(mockProperties).sender();
		verify(mockProperties).recipients();
		verify(mockDbService).getUnreportedFallouts();
		verify(mockMessagingClient).sendEmailBatch(eq("2281"), any(EmailBatchRequest.class));
		verify(mockDbService).markAllFalloutsAsReported();
		verify(mockFalloutMapper).createEmailBatchRequest(Mockito.anyList());
		verifyNoMoreInteractions(mockProperties, mockDbService, mockFalloutMapper, mockFalloutMailTemplate, mockMessagingClient);
	}
}
