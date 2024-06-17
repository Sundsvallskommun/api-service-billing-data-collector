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
@Table(name = "scheduled_job_log")
public class ScheduledJobEntity {

	@Id
	@Column(name = "id")
	@UuidGenerator
	private String id;

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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof ScheduledJobEntity that)) return false;
		return Objects.equals(id, that.id) && Objects.equals(fetchedStartDate, that.fetchedStartDate) && Objects.equals(fetchedEndDate, that.fetchedEndDate) && Objects.equals(processed, that.processed);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, fetchedStartDate, fetchedEndDate, processed);
	}

	@Override
	public String toString() {
		return "ScheduledJobEntity{" +
			"id='" + id + '\'' +
			", fetchedStartDate=" + fetchedStartDate +
			", fetchedEndDate=" + fetchedEndDate +
			", processed=" + processed +
			'}';
	}
}
