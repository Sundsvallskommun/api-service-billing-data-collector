package se.sundsvall.billingdatacollector.integration.db.converter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class IntegerSetConverterTest {

	private final IntegerSetConverter converter = new IntegerSetConverter();

	@Test
	void convertToDatabaseColumnWithValidSet() {
		// Arrange
		final var attribute = Set.of(1, 15, 31);

		// Act
		final var result = converter.convertToDatabaseColumn(attribute);

		// Assert
		assertThat(result.split(",")).containsExactlyInAnyOrder("1", "15", "31");
	}

	@ParameterizedTest
	@NullAndEmptySource
	void convertToDatabaseColumnWithNullOrEmpty(Set<Integer> attribute) {
		// Act
		final var result = converter.convertToDatabaseColumn(attribute);

		// Assert
		assertThat(result).isEmpty();
	}

	@Test
	void convertToEntityAttributeWithValidString() {
		// Arrange
		final var dbData = "1,15,31";

		// Act
		final var result = converter.convertToEntityAttribute(dbData);

		// Assert
		assertThat(result).containsExactlyInAnyOrder(1, 15, 31);
	}

	@ParameterizedTest
	@NullAndEmptySource
	void convertToEntityAttributeWithNullOrEmpty(String dbData) {
		// Act
		final var result = converter.convertToEntityAttribute(dbData);

		// Assert
		assertThat(result)
			.isNotNull()
			.isEmpty();
	}

	@Test
	void roundTrip_shouldMaintainDataIntegrity() {
		final var original = Set.of(1, 2, 3, 4, 5, 10, 12);

		final var dbValue = converter.convertToDatabaseColumn(original);
		final var result = converter.convertToEntityAttribute(dbValue);

		assertThat(result).isEqualTo(original);
	}
}
