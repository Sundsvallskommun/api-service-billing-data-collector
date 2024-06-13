package se.sundsvall.billingdatacollector.integration.db.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

	@Column(name = "fetched_start_date", nullable = false)
	private LocalDate fetchedStartDate;

	@Column(name = "fetched_end_date", nullable = false)
	private LocalDate fetchedEndDate;

	/**
	 * Date when the job was processed.
	 */
	@Column(name = "processed", nullable = false)
	private LocalDateTime processed;

	@PrePersist
	public void prePersist() {
		if (processed == null) {
			processed = LocalDateTime.now();
		}
	}
}
