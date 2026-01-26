package se.sundsvall.billingdatacollector.service.scheduling.certificate.model;

import java.util.Objects;

/**
 * Helper class for monitoring health status and display unhealthy message
 */
public class Health {
	private boolean healthy;
	private String message;

	public static Health create() {
		return new Health();
	}

	public void setHealthy(boolean healthy) {
		this.healthy = healthy;
	}

	public Health withHealthy(boolean healthy) {
		this.healthy = healthy;
		return this;
	}

	public boolean isHealthy() {
		return healthy;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public Health withMessage(String message) {
		this.message = message;
		return this;
	}

	public String getMessage() {
		return message;
	}

	@Override
	public int hashCode() {
		return Objects.hash(healthy, message);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final Health other)) { return false; }
		return healthy == other.healthy && Objects.equals(message, other.message);
	}

	@Override
	public String toString() {
		final var builder = new StringBuilder();
		builder.append("Health [healthy=").append(healthy).append(", message=").append(message).append("]");
		return builder.toString();
	}
}
