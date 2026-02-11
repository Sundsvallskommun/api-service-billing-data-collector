package se.sundsvall.billingdatacollector.integration.db.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
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
		@Index(name = "idx_stakeholder_type", columnList = "stakeholder_type")
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_legal_id_pattern",
			columnNames = {
				"legal_id_pattern"
			})
	})
public class CounterpartMappingEntity {

	@Id
	@UuidGenerator
	@Column(name = "id")
	private String id;

	@Column(name = "legal_id_pattern", length = 12)
	private String legalIdPattern;

	@Column(name = "stakeholder_type", length = 20)
	private String stakeholderType;

	@Column(name = "counterpart", nullable = false, length = 5)
	private String counterpart;

	@Override
	public int hashCode() {
		return Objects.hash(counterpart, id, legalIdPattern, stakeholderType);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (!(obj instanceof final CounterpartMappingEntity other)) { return false; }
		return Objects.equals(counterpart, other.counterpart) && Objects.equals(id, other.id)
			&& Objects.equals(legalIdPattern, other.legalIdPattern) && Objects.equals(stakeholderType, other.stakeholderType);
	}

	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("CounterpartMappingEntity [id=").append(id)
			.append(", legalIdPattern=").append(legalIdPattern)
			.append(", stakeholderType=").append(stakeholderType)
			.append(", counterpart=").append(counterpart).append("]");
		return builder.toString();
	}
}
