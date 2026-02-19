package se.sundsvall.billingdatacollector.service.scheduling.certificate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@ExtendWith(MockitoExtension.class)
class CertificateValidityCheckSchedulerTest {

	@Mock
	private CertificateValidityCheckHandler mockCertificateValidityCheckHandler;

	@InjectMocks
	private CertificateValidityCheckScheduler certificateValidityCheckScheduler;

	@Test
	void executeJob() {
		// Act
		certificateValidityCheckScheduler.execute();

		// Assert
		verify(mockCertificateValidityCheckHandler).checkCertificateHealth();
		verifyNoMoreInteractions(mockCertificateValidityCheckHandler);
	}
}
