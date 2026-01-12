package se.sundsvall.billingdatacollector.api.model;

import static io.swagger.v3.oas.annotations.media.Schema.AccessMode.READ_ONLY;
import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Scheduled billing")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder(setterPrefix = "with")
public class ScheduledBilling {

	@Schema(description = "Unique id for scheduled billing", examples = "f0882f1d-06bc-47fd-b017-1d8307f5ce95", accessMode = READ_ONLY)
	private String id;

	@Schema(description = "External id in source system", example = "66c57446-72e7-4cc5-af7c-053919ce904b", requiredMode = REQUIRED)
	@NotNull
	private String externalId;

	@Schema(description = "Source system where billing data is collected", example = "CONTRACT", requiredMode = REQUIRED)
	@NotNull
	private BillingSource source;

	@Schema(description = "Day of month when billing should be scheduled. On short months, highest possible date will be used if value is bigger than number of days in month", example = "1", minimum = "1", maximum = "31", requiredMode = REQUIRED)
	@Min(1)
	@Max(31)
	private Integer invoicingDate;

	@ArraySchema(schema = @Schema(implementation = Integer.class, description = "Which months billing should be scheduled", example = "1", minimum = "1", maximum = "12", requiredMode = REQUIRED))
	@NotNull
	private Set<@Min(1) @Max(12) Integer> invoicingMonths;

	@Schema(description = "Timestamp when of last successful billing", example = "2000-10-31T01:30:00.000+02:00", accessMode = READ_ONLY)
	private OffsetDateTime lastBilled;

	@Schema(description = "Date of next scheduled billing if still active", example = "2001-05-15", accessMode = READ_ONLY)
	private LocalDate nextScheduledBilling;
}
