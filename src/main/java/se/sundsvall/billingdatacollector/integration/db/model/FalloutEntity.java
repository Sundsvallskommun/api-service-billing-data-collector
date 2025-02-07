package se.sundsvall.billingdatacollector.integration.db.model;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Length;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;
import se.sundsvall.billingdatacollector.integration.db.converter.BillingRecordWrapperConverter;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

@Getter
@Setter
@Builder(setterPrefix = "with")
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
	name = "fallout",
	indexes = {
		@Index(name = "idx_family_id", columnList = "family_id"),
		@Index(name = "idx_flow_instance_id", columnList = "flow_instance_id"),
		@Index(name = "idx_municipality_id", columnList = "municipality_id")
	})
public class FalloutEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 4)
	private String municipalityId;

	@Column(name = "request_id", length = 36)
	private String requestId;

	@Column(name = "billing_record_wrapper", length = Length.LONG)
	@Convert(converter = BillingRecordWrapperConverter.class)
	private BillingRecordWrapper billingRecordWrapper;

	@Column(name = "opene_instance", length = Length.LONG)
	private String openEInstance;

	@Column(name = "family_id")
	private String familyId;

	@Column(name = "flow_instance_id")
	private String flowInstanceId;

	@Column(name = "error_message", length = 1024)
	private String errorMessage;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@Column(name = "reported", columnDefinition = "boolean default false")
	private boolean reported;

	@PreUpdate
	@PrePersist
	public void prePersist() {
		if (created == null) {
			created = OffsetDateTime.now();
		}
		modified = OffsetDateTime.now();
	}

	@Override
	public int hashCode() {
		return Objects.hash(billingRecordWrapper, created, errorMessage, familyId, flowInstanceId, id, modified, municipalityId, openEInstance, reported, requestId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final FalloutEntity other)) { return false; }
		return Objects.equals(billingRecordWrapper, other.billingRecordWrapper) && Objects.equals(created, other.created) && Objects.equals(errorMessage, other.errorMessage) && Objects.equals(familyId, other.familyId) && Objects.equals(flowInstanceId,
			other.flowInstanceId) && Objects.equals(id, other.id) && Objects.equals(modified, other.modified) && Objects.equals(municipalityId, other.municipalityId) && Objects.equals(openEInstance, other.openEInstance) && (reported == other.reported)
			&& Objects.equals(requestId, other.requestId);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("FalloutEntity [id=").append(id).append(", municipalityId=").append(municipalityId).append(", requestId=").append(requestId).append(", billingRecordWrapper=").append(billingRecordWrapper).append(", openEInstance=").append(
			openEInstance).append(", familyId=").append(familyId).append(", flowInstanceId=").append(flowInstanceId).append(", errorMessage=").append(errorMessage).append(", created=").append(created).append(", modified=").append(modified).append(
				", reported=").append(reported).append("]");
		return builder.toString();
	}
}
