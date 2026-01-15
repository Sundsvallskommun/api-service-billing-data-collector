package se.sundsvall.billingdatacollector.integration.db.model;

import static org.hibernate.annotations.TimeZoneStorageType.NORMALIZE;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.TimeZoneStorage;
import se.sundsvall.billingdatacollector.api.model.BillingSource;
import se.sundsvall.billingdatacollector.integration.db.converter.IntegerSetConverter;

@Entity
@Table(
	name = "scheduled_billing",
	indexes = {
		@Index(name = "idx_municipality_id_external_id_source", columnList = "municipality_id,external_id,source"),
		@Index(name = "idx_next_scheduled_billing_paused", columnList = "next_scheduled_billing,paused")
	},
	uniqueConstraints = {
		@UniqueConstraint(
			name = "uq_external_id_municipality_source",
			columnNames = {
				"external_id", "municipality_id", "source"
			})
	})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class ScheduledBillingEntity {
	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private String id;

	@Column(name = "municipality_id", nullable = false, length = 4)
	private String municipalityId;

	@Column(name = "external_id", nullable = false, length = 64)
	private String externalId;

	@Enumerated(EnumType.STRING)
	@Column(name = "source", nullable = false)
	private BillingSource source;

	@Convert(converter = IntegerSetConverter.class)
	@Column(name = "billing_days_of_month", nullable = false)
	private Set<Integer> billingDaysOfMonth;

	@Convert(converter = IntegerSetConverter.class)
	@Column(name = "billing_months", nullable = false)
	private Set<Integer> billingMonths;

	@Column(name = "last_billed")
	@TimeZoneStorage(NORMALIZE)
	private OffsetDateTime lastBilled;

	@Column(name = "next_scheduled_billing")
	private LocalDate nextScheduledBilling;

	@Builder.Default
	@Column(nullable = false)
	private boolean paused = false;
}
