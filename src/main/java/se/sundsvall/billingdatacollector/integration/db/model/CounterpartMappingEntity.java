package se.sundsvall.billingdatacollector.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

@Getter
@Setter
@Builder(setterPrefix = "with")
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
	name = "counterpart_mapping",
	indexes = {
		@Index(name = "idx_legal_id", columnList = "legal_id"),
		@Index(name = "idx_legal_id_pattern", columnList = "legal_id_pattern"),
		@Index(name = "idx_stakeholder_type", columnList = "stakeholder_type")
	})
public class CounterpartMappingEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "legal_id", unique = true)
	private String legalId;

	@Column(name = "legal_id_pattern", unique = true)
	private String legalIdPattern;

	@Column(name = "stakeholder_type")
	private String stakeholderType;

	@Column(name = "counterpart", nullable = false, unique = true)
	private String counterpart;

	@Override
	public int hashCode() {
		return Objects.hash(counterpart, id, legalId, legalIdPattern, stakeholderType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final CounterpartMappingEntity other)) { return false; }
		return Objects.equals(counterpart, other.counterpart) && Objects.equals(id, other.id) && Objects.equals(legalId, other.legalId)
			&& Objects.equals(legalIdPattern, other.legalIdPattern) && Objects.equals(stakeholderType, other.stakeholderType);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CounterpartMappingEntity [id=").append(id).append(", legalId=").append(legalId)
			.append(", legalIdPattern=").append(legalIdPattern).append(", stakeholderType=").append(stakeholderType)
			.append(", counterpart=").append(counterpart).append("]");
		return builder.toString();
	}
}
