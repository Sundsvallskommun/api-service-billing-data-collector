package se.sundsvall.billingdatacollector.integration.db.model;

import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanConstructor;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanEqualsExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanHashCodeExcluding;
import static com.google.code.beanmatchers.BeanMatchers.hasValidBeanToString;
import static com.google.code.beanmatchers.BeanMatchers.hasValidGettersAndSetters;
import static com.google.code.beanmatchers.BeanMatchers.registerValueGenerator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.MatcherAssert.assertThat;

import java.time.OffsetDateTime;
import java.util.Random;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

class FalloutEntityTest {

	@BeforeAll
	static void setup() {
		registerValueGenerator(() -> OffsetDateTime.now().plusDays(new Random().nextInt()), OffsetDateTime.class);
	}

	@Test
	void testBean() {
		assertThat(FalloutEntity.class, allOf(
			hasValidBeanConstructor(),
			hasValidGettersAndSetters(),
			hasValidBeanHashCodeExcluding("billingRecordWrapper", "openEInstance", "errorMessage"),
			hasValidBeanEqualsExcluding("billingRecordWrapper", "openEInstance", "errorMessage"),
			hasValidBeanToString()));
	}

	@Test
	void testBuilderMethods() {
		var id = UUID.randomUUID().toString();
		var requestId = UUID.randomUUID().toString();
		var billingRecordWrapper = BillingRecordWrapper.builder().build();
		var openEInstance = "some xml";
		var familyId = "familyId";
		var flowInstanceId = "flowInstanceId";
		var created = OffsetDateTime.now();
		var modified = OffsetDateTime.now();
		var errorMessage = "an error message";

		var entity = FalloutEntity.builder()
			.withId(id)
			.withRequestId(requestId)
			.withBillingRecordWrapper(billingRecordWrapper)
			.withOpenEInstance(openEInstance)
			.withFamilyId(familyId)
			.withFlowInstanceId(flowInstanceId)
			.withCreated(created)
			.withModified(modified)
			.withErrorMessage(errorMessage)
			.build();

		assertThat(entity).isNotNull().hasNoNullFieldsOrProperties();
		assertThat(entity.getId()).isEqualTo(id);
		assertThat(entity.getRequestId()).isEqualTo(requestId);
		assertThat(entity.getBillingRecordWrapper()).isEqualTo(billingRecordWrapper);
		assertThat(entity.getOpenEInstance()).isEqualTo(openEInstance);
		assertThat(entity.getFamilyId()).isEqualTo(familyId);
		assertThat(entity.getFlowInstanceId()).isEqualTo(flowInstanceId);
		assertThat(entity.getCreated()).isEqualTo(created);
		assertThat(entity.getModified()).isEqualTo(modified);
		assertThat(entity.getErrorMessage()).isEqualTo(errorMessage);
	}

	@Test
	void testNoDirt() {
		assertThat(FalloutEntity.builder().build()).hasAllNullFieldsOrProperties();
		assertThat(new FalloutEntity()).hasAllNullFieldsOrProperties();
	}

	@Test
	void testPrePersist() {
		var entity = FalloutEntity.builder().build();
		assertThat(entity.getCreated()).isNull();
		assertThat(entity.getModified()).isNull();

		entity.prePersist();

		assertThat(entity.getCreated()).isBeforeOrEqualTo(OffsetDateTime.now());
		assertThat(entity.getModified()).isBeforeOrEqualTo(OffsetDateTime.now());
		assertThat(entity).hasAllNullFieldsOrPropertiesExcept("created", "modified");
	}

	@Test
	void testPrePersist_shouldNotUpdateCreated () {
		var entity = FalloutEntity.builder().build();
		entity.prePersist();
		var created = entity.getCreated();
		entity.prePersist();
		assertThat(entity.getCreated()).isEqualTo(created);
	}
}