package se.sundsvall.billingdatacollector.service.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static se.sundsvall.dept44.util.LogUtils.sanitizeForLogging;

public abstract class AbstractHandler implements BillingSourceHandler {
	private final Logger logger;

	protected AbstractHandler() {
		this.logger = LoggerFactory.getLogger(getClass());
	}

	protected void logInfo(String message) {
		if (logger.isInfoEnabled()) {
			logger.info(sanitizeForLogging(message));
		}
	}

	protected void logInfo(String message, Object... objects) {
		if (logger.isInfoEnabled()) {
			logger.info(sanitizeForLogging(message), objects);
		}
	}

	protected void logWarning(String message) {
		if (logger.isWarnEnabled()) {
			logger.warn(sanitizeForLogging(message));
		}
	}

	protected void logWarning(String message, Object... objects) {
		if (logger.isWarnEnabled()) {
			logger.warn(sanitizeForLogging(message), objects);
		}
	}

	protected void logError(String message) {
		if (logger.isErrorEnabled()) {
			logger.error(sanitizeForLogging(message));
		}
	}

	protected void logError(String message, Object... objects) {
		if (logger.isErrorEnabled()) {
			logger.error(sanitizeForLogging(message), objects);
		}
	}
}
