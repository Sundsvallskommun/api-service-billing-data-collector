package se.sundsvall.billingdatacollector.integration.db.model;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import java.time.OffsetDateTime;
import java.util.Objects;

import org.hibernate.Length;
import org.hibernate.annotations.TimeZoneStorage;
import org.hibernate.annotations.UuidGenerator;

import se.sundsvall.billingdatacollector.integration.db.converter.BillingRecordWrapperConverter;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "fallout")
public class FalloutEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "billing_record_wrapper", length = Length.LONG)
	@Convert(converter = BillingRecordWrapperConverter.class)
	private BillingRecordWrapper billingRecordWrapper;

	@Column(name = "opene_instance", length = Length.LONG)
	private String openEInstance;

	@Column(name = "family_id")
	private String familyId;

	@Column(name = "flow_instance_id")
	private String flowInstanceId;

	@Column(name = "error_message")
	private String errorMessage;

	@Column(name = "created")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime created;

	@Column(name = "modified")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime modified;

	@PreUpdate
	@PrePersist
	public void prePersist() {
		if (created == null) {
			created = OffsetDateTime.now();
		}
		modified = OffsetDateTime.now();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof FalloutEntity that)) return false;
		return Objects.equals(id, that.id) && Objects.deepEquals(openEInstance, that.openEInstance) && Objects.equals(familyId, that.familyId) && Objects.equals(flowInstanceId, that.flowInstanceId) && Objects.equals(created, that.created) && Objects.equals(modified, that.modified);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, familyId, flowInstanceId, created, modified);
	}

	@Override
	public String toString() {
		return "FalloutEntity{" +
			"id='" + id + '\'' +
			", billingRecordWrapper=" + billingRecordWrapper +
			", openEInstance=" + openEInstance +
			", familyId='" + familyId + '\'' +
			", flowInstanceId='" + flowInstanceId + '\'' +
			", errorMessage='" + errorMessage + '\'' +
			", created=" + created +
			", modified=" + modified +
			'}';
	}
}