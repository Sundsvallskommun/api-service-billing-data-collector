package se.sundsvall.billingdatacollector.integration.db.model;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder(setterPrefix = "with")
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
	name = "scheduled_job_log",
	indexes = {
		@Index(name = "idx_municipality_id", columnList = "municipality_id")
	})
public class ScheduledJobEntity {

	@Id
	@Column(name = "id")
	@UuidGenerator
	private String id;

	@Column(name = "municipality_id")
	private String municipalityId;

	/**
	 * Which start date was used when we fetched data.
	 */
	@Column(name = "fetched_start_date", nullable = false)
	private LocalDate fetchedStartDate;

	/**
	 * Which end date was used when we fetched data.
	 */
	@Column(name = "fetched_end_date", nullable = false)
	private LocalDate fetchedEndDate;

	/**
	 * Date when the job was processed.
	 */
	@Column(name = "processed", nullable = false)
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime processed;

	@PrePersist
	public void prePersist() {
		if (processed == null) {
			processed = OffsetDateTime.now();
		}
	}

	@Override
	public int hashCode() {
		return Objects.hash(fetchedEndDate, fetchedStartDate, id, municipalityId, processed);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final ScheduledJobEntity other)) { return false; }
		return Objects.equals(fetchedEndDate, other.fetchedEndDate) && Objects.equals(fetchedStartDate, other.fetchedStartDate) && Objects.equals(id, other.id) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(processed,
			other.processed);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("ScheduledJobEntity [id=").append(id).append(", municipalityId=").append(municipalityId).append(", fetchedStartDate=").append(fetchedStartDate).append(", fetchedEndDate=").append(fetchedEndDate).append(", processed=").append(
			processed).append("]");
		return builder.toString();
	}
}
