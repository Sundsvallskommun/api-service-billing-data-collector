package se.sundsvall.billingdatacollector.integration.db.converter;

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.PersistenceException;

/**
 * Convert {@link BillingRecordWrapper} to and from JSON.
 **/
public class BillingRecordWrapperConverter implements AttributeConverter<BillingRecordWrapper, String> {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper().registerModule(new JavaTimeModule());

	@Override
	public String convertToDatabaseColumn(final BillingRecordWrapper billingRecordWrapper) {
		return ofNullable(billingRecordWrapper)
			.map(wrapper -> {
				try {
					return OBJECT_MAPPER.writeValueAsString(billingRecordWrapper);
				} catch (Exception e) {
					throw new PersistenceException("Unable to serialize billing data wrapper", e);
				}
			})
			.orElse(null);
	}

	@Override
	public BillingRecordWrapper convertToEntityAttribute(final String json) {
		return ofNullable(json)
			.filter(not(String::isBlank))
			.map(s -> {
				try {
					return OBJECT_MAPPER.readValue(json, BillingRecordWrapper.class);
				} catch (Exception e) {
					throw new PersistenceException("Unable to deserialize billing data wrapper", e);
				}
			})
			.orElse(null);
	}
}
