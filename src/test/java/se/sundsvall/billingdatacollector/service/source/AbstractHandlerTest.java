package se.sundsvall.billingdatacollector.service.source;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class AbstractHandlerTest {
	private static final String MESSAGE = "message";

	@Mock
	private Logger loggerMock;

	@AfterEach
	void verifyNoMoreMockInteraction() {
		verifyNoMoreInteractions(loggerMock);
	}

	@Test
	void logInfoWhenEnabled() {
		try (var loggerFactoryMock = mockStatic(LoggerFactory.class)) {
			// Arrange
			loggerFactoryMock.when(() -> LoggerFactory.getLogger(any(DummyHandler.class.getClass()))).thenReturn(loggerMock);
			when(loggerMock.isInfoEnabled()).thenReturn(true);

			// Act
			final var dummyHandler = new DummyHandler();
			dummyHandler.logInfo(MESSAGE);

			// Verify
			verify(loggerMock).isInfoEnabled();
			verify(loggerMock).info(MESSAGE);

		}
	}

	@Test
	void logInfoWhenDisabled() {
		try (var loggerFactoryMock = mockStatic(LoggerFactory.class)) {
			// Arrange
			loggerFactoryMock.when(() -> LoggerFactory.getLogger(any(DummyHandler.class.getClass()))).thenReturn(loggerMock);

			// Act
			final var dummyHandler = new DummyHandler();
			dummyHandler.logInfo(MESSAGE);

			// Verify
			verify(loggerMock).isInfoEnabled();
			verify(loggerMock, never()).info(MESSAGE);

		}
	}

	@Test
	void logWarningWhenEnabled() {
		try (var loggerFactoryMock = mockStatic(LoggerFactory.class)) {
			// Arrange
			loggerFactoryMock.when(() -> LoggerFactory.getLogger(any(DummyHandler.class.getClass()))).thenReturn(loggerMock);
			when(loggerMock.isWarnEnabled()).thenReturn(true);

			// Act
			final var dummyHandler = new DummyHandler();
			dummyHandler.logWarning(MESSAGE);

			// Verify
			verify(loggerMock).isWarnEnabled();
			verify(loggerMock).warn(MESSAGE);

		}
	}

	@Test
	void logWarningWhenDisabled() {
		try (var loggerFactoryMock = mockStatic(LoggerFactory.class)) {
			// Arrange
			loggerFactoryMock.when(() -> LoggerFactory.getLogger(any(DummyHandler.class.getClass()))).thenReturn(loggerMock);

			// Act
			final var dummyHandler = new DummyHandler();
			dummyHandler.logWarning(MESSAGE);

			// Verify
			verify(loggerMock).isWarnEnabled();
			verify(loggerMock, never()).warn(MESSAGE);

		}
	}

	@Test
	void logErrorWhenEnabled() {
		try (var loggerFactoryMock = mockStatic(LoggerFactory.class)) {
			// Arrange
			loggerFactoryMock.when(() -> LoggerFactory.getLogger(any(DummyHandler.class.getClass()))).thenReturn(loggerMock);
			when(loggerMock.isErrorEnabled()).thenReturn(true);

			// Act
			final var dummyHandler = new DummyHandler();
			dummyHandler.logError(MESSAGE);

			// Verify
			verify(loggerMock).isErrorEnabled();
			verify(loggerMock).error(MESSAGE);

		}
	}

	@Test
	void logErrorWhenDisabled() {
		try (var loggerFactoryMock = mockStatic(LoggerFactory.class)) {
			// Arrange
			loggerFactoryMock.when(() -> LoggerFactory.getLogger(any(DummyHandler.class.getClass()))).thenReturn(loggerMock);

			// Act
			final var dummyHandler = new DummyHandler();
			dummyHandler.logError(MESSAGE);

			// Verify
			verify(loggerMock).isErrorEnabled();
			verify(loggerMock, never()).error(MESSAGE);

		}
	}

	private static class DummyHandler extends AbstractHandler {
		@Override
		public void sendBillingRecords(String municipalityId, String externalId) {
			// Not implemented as dummy class is only used by test
		}
	}
}
