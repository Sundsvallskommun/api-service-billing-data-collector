package se.sundsvall.billingdatacollector.integration.db.model;

import java.time.LocalDate;
import java.util.Objects;

import org.hibernate.Length;
import org.hibernate.annotations.UuidGenerator;

import se.sundsvall.billingdatacollector.integration.db.converter.BillingRecordWrapperConverter;
import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

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
	}
)
public class HistoryEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

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
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof HistoryEntity that)) return false;
		return Objects.equals(id, that.id) && Objects.equals(familyId, that.familyId) && Objects.equals(flowInstanceId, that.flowInstanceId) && Objects.equals(created, that.created) && Objects.equals(location, that.location) && Objects.equals(requestId, that.requestId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, familyId, flowInstanceId, created, location, requestId);
	}

	@Override
	public String toString() {
		return "HistoryEntity{" +
			"id='" + id + '\'' +
			", billingRecordWrapper=" + billingRecordWrapper +
			", familyId='" + familyId + '\'' +
			", flowInstanceId='" + flowInstanceId + '\'' +
			", created=" + created +
			", location='" + location + '\'' +
			", requestId='" + requestId + '\'' +
			'}';
	}
}
