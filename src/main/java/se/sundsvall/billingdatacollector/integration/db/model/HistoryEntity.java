package se.sundsvall.billingdatacollector.integration.db.model;

import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.Length;
import org.hibernate.annotations.UuidGenerator;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
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
import se.sundsvall.billingdatacollector.integration.db.converter.BillingRecordWrapperConverter;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

@Getter
@Setter
@Builder(setterPrefix = "with")
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
	name = "history",
	indexes = {
		@Index(name = "idx_family_id", columnList = "family_id"),
		@Index(name = "idx_flow_instance_id", columnList = "flow_instance_id"),
		@Index(name = "idx_municipality_id", columnList = "municipality_id")
	})
public class HistoryEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id")
	private String municipalityId;

	@Column(name = "request_id", length = 36)
	private String requestId;

	@Column(name = "billing_record_wrapper", length = Length.LONG)
	@Convert(converter = BillingRecordWrapperConverter.class)
	private BillingRecordWrapper billingRecordWrapper;

	@Column(name = "family_id")
	private String familyId;

	@Column(name = "flow_instance_id")
	private String flowInstanceId;

	@Column(name = "created")
	private LocalDate created;

	@Column(name = "location")
	private String location;

	@PrePersist
	public void prePersist() {
		created = LocalDate.now();
	}

	@Override
	public int hashCode() {
		return Objects.hash(billingRecordWrapper, created, familyId, flowInstanceId, id, location, municipalityId, requestId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final HistoryEntity other)) { return false; }
		return Objects.equals(billingRecordWrapper, other.billingRecordWrapper) && Objects.equals(created, other.created) && Objects.equals(familyId, other.familyId) && Objects.equals(flowInstanceId, other.flowInstanceId) && Objects.equals(id, other.id)
			&& Objects.equals(location, other.location) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(requestId, other.requestId);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("HistoryEntity [id=").append(id).append(", municipalityId=").append(municipalityId).append(", requestId=").append(requestId).append(", billingRecordWrapper=").append(billingRecordWrapper).append(", familyId=").append(familyId)
			.append(", flowInstanceId=").append(flowInstanceId).append(", created=").append(created).append(", location=").append(location).append("]");
		return builder.toString();
	}
}
