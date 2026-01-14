package se.sundsvall.billingdatacollector.integration.db.converter;

import jakarta.persistence.AttributeConverter;
import java.util.Set;

public class IntegerSetConverter implements AttributeConverter<Set<Integer>, String> {
	@Override
	public String convertToDatabaseColumn(Set<Integer> attribute) {
		if (attribute == null || attribute.isEmpty())
			return "";
		return attribute.stream()
			.map(String::valueOf)
			.collect(java.util.stream.Collectors.joining(","));
	}

	@Override
	public Set<Integer> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isEmpty())
			return java.util.Collections.emptySet();
		return java.util.Arrays.stream(dbData.split(","))
			.map(Integer::parseInt)
			.collect(java.util.stream.Collectors.toSet());
	}
}
