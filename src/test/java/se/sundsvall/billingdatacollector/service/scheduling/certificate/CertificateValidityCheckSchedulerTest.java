package se.sundsvall.billingdatacollector.service.scheduling.certificate;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
