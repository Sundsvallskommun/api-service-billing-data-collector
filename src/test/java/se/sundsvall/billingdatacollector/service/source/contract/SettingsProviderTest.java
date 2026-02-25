package se.sundsvall.billingdatacollector.service.source.contract;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.LeaseType;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static generated.se.sundsvall.contract.ContractType.LAND_LEASE_PUBLIC;
import static generated.se.sundsvall.contract.LeaseType.LAND_LEASE_MISC;
import static generated.se.sundsvall.contract.LeaseType.LAND_LEASE_RESIDENTIAL;
import static generated.se.sundsvall.contract.LeaseType.OBJECT_LEASE;
import static generated.se.sundsvall.contract.LeaseType.OTHER_FEE;
import static generated.se.sundsvall.contract.LeaseType.SITE_LEASE_COMMERCIAL;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_FARMING;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_HUNTING;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_MISC;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_MOORING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@SpringBootTest(classes = SettingsProvider.class, webEnvironment = MOCK)
class SettingsProviderTest {
	private static final String ACTIVITY_3091 = "3091";
	private static final String ACTIVITY_3092 = "3092";
	private static final String ACTIVITY_3093 = "3093";
	private static final String ACTIVITY_3099 = "3099";
	private static final String COST_CENTER_36000000 = "36000000";
	private static final String DEPARTMENT_810100 = "810100";
	private static final String SUB_ACCOUNT_342000 = "342000";
	private static final String SUB_ACCOUNT_342100 = "342100";
	private static final String VATCODE_00 = "00";
	private static final String VATCODE_25 = "25";

	@Autowired
	private SettingsProvider settingsProvider;

	@Test
	void componentIsPresent() {
		assertThat(settingsProvider).isNotNull();
	}

	@ParameterizedTest
	@EnumSource(value = LeaseType.class)
	void isLeaseTypeSettingsPresent(LeaseType leaseType) {
		assertThat(settingsProvider.isLeaseTypeSettingsPresent(new Contract().leaseType(leaseType))).isTrue();
	}

	void isLeaseTypeSettingsPresentForNull() {
		assertThat(settingsProvider.isLeaseTypeSettingsPresent(new Contract())).isFalse();
	}

	@Test
	void isLeaseTypeSettingsPresentForContractType() {
		assertThat(settingsProvider.isLeaseTypeSettingsPresent(new Contract().type(LAND_LEASE_PUBLIC))).isTrue();
	}

	@ParameterizedTest
	@MethodSource("getActivityArgumentProvider")
	void getActivity(LeaseType leaseType, String expectedValue) {
		assertThat(settingsProvider.getActivity(new Contract().leaseType(leaseType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getActivityArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, ACTIVITY_3091),
			// Arguments.of(LAND_LEASE_PUBLIC, ACTIVITY_3093),
			Arguments.of(LAND_LEASE_RESIDENTIAL, ACTIVITY_3091),
			// Arguments.of(LEASEHOLD, ACTIVITY_3092),
			Arguments.of(OBJECT_LEASE, ACTIVITY_3091),
			Arguments.of(OTHER_FEE, ACTIVITY_3091),
			Arguments.of(SITE_LEASE_COMMERCIAL, ACTIVITY_3091),
			Arguments.of(USUFRUCT_FARMING, ACTIVITY_3091),
			Arguments.of(USUFRUCT_HUNTING, ACTIVITY_3091),
			Arguments.of(USUFRUCT_MISC, ACTIVITY_3091),
			Arguments.of(USUFRUCT_MOORING, ACTIVITY_3099),
			Arguments.of(null, null));
	}

	@ParameterizedTest
	@MethodSource("getCostCenterArgumentProvider")
	void getCostCenter(LeaseType leaseType, String expectedValue) {
		assertThat(settingsProvider.getCostCenter(new Contract().leaseType(leaseType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getCostCenterArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, COST_CENTER_36000000),
			// Arguments.of(LAND_LEASE_PUBLIC, COST_CENTER_36000000),
			Arguments.of(LAND_LEASE_RESIDENTIAL, COST_CENTER_36000000),
			// Arguments.of(LEASEHOLD, COST_CENTER_36000000),
			Arguments.of(OBJECT_LEASE, COST_CENTER_36000000),
			Arguments.of(OTHER_FEE, COST_CENTER_36000000),
			Arguments.of(SITE_LEASE_COMMERCIAL, COST_CENTER_36000000),
			Arguments.of(USUFRUCT_FARMING, COST_CENTER_36000000),
			Arguments.of(USUFRUCT_HUNTING, COST_CENTER_36000000),
			Arguments.of(USUFRUCT_MISC, COST_CENTER_36000000),
			Arguments.of(USUFRUCT_MOORING, COST_CENTER_36000000),
			Arguments.of(null, null));
	}

	@ParameterizedTest
	@MethodSource("getDepartmentArgumentProvider")
	void getDepartment(LeaseType leaseType, String expectedValue) {
		assertThat(settingsProvider.getDepartment(new Contract().leaseType(leaseType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getDepartmentArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, DEPARTMENT_810100),
			// Arguments.of(LAND_LEASE_PUBLIC, DEPARTMENT_810100),
			Arguments.of(LAND_LEASE_RESIDENTIAL, DEPARTMENT_810100),
			// Arguments.of(LEASEHOLD, DEPARTMENT_810100),
			Arguments.of(OBJECT_LEASE, DEPARTMENT_810100),
			Arguments.of(OTHER_FEE, DEPARTMENT_810100),
			Arguments.of(SITE_LEASE_COMMERCIAL, DEPARTMENT_810100),
			Arguments.of(USUFRUCT_FARMING, DEPARTMENT_810100),
			Arguments.of(USUFRUCT_HUNTING, DEPARTMENT_810100),
			Arguments.of(USUFRUCT_MISC, DEPARTMENT_810100),
			Arguments.of(USUFRUCT_MOORING, DEPARTMENT_810100),
			Arguments.of(null, null));
	}

	@ParameterizedTest
	@MethodSource("getSubaccountArgumentProvider")
	void getSubaccount(LeaseType leaseType, String expectedValue) {
		assertThat(settingsProvider.getSubaccount(new Contract().leaseType(leaseType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getSubaccountArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, SUB_ACCOUNT_342000),
			// Arguments.of(LAND_LEASE_PUBLIC, SUB_ACCOUNT_342000),
			Arguments.of(LAND_LEASE_RESIDENTIAL, SUB_ACCOUNT_342000),
			// Arguments.of(LEASEHOLD, SUB_ACCOUNT_342000),
			Arguments.of(OBJECT_LEASE, SUB_ACCOUNT_342000),
			Arguments.of(OTHER_FEE, SUB_ACCOUNT_342000),
			Arguments.of(SITE_LEASE_COMMERCIAL, SUB_ACCOUNT_342000),
			Arguments.of(USUFRUCT_FARMING, SUB_ACCOUNT_342100),
			Arguments.of(USUFRUCT_HUNTING, SUB_ACCOUNT_342100),
			Arguments.of(USUFRUCT_MISC, SUB_ACCOUNT_342000),
			Arguments.of(USUFRUCT_MOORING, SUB_ACCOUNT_342000),
			Arguments.of(null, null));
	}

	@ParameterizedTest
	@MethodSource("getVatCodeArgumentProvider")
	void getVatCode(LeaseType leaseType, String expectedValue) {
		assertThat(settingsProvider.getVatCode(new Contract().leaseType(leaseType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getVatCodeArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, VATCODE_00),
			// Arguments.of(LAND_LEASE_PUBLIC, VATCODE_00),
			Arguments.of(LAND_LEASE_RESIDENTIAL, VATCODE_00),
			// Arguments.of(LEASEHOLD, VATCODE_00),
			Arguments.of(OBJECT_LEASE, VATCODE_00),
			Arguments.of(OTHER_FEE, VATCODE_00),
			Arguments.of(SITE_LEASE_COMMERCIAL, VATCODE_00),
			Arguments.of(USUFRUCT_FARMING, VATCODE_25),
			Arguments.of(USUFRUCT_HUNTING, VATCODE_25),
			Arguments.of(USUFRUCT_MISC, VATCODE_00),
			Arguments.of(USUFRUCT_MOORING, VATCODE_00),
			Arguments.of(null, null));
	}

	@Test
	void getActivityFallbackToContractType() {
		assertThat(settingsProvider.getActivity(new Contract().type(LAND_LEASE_PUBLIC))).isEqualTo(ACTIVITY_3093);
	}

	@Test
	void getCostCenterFallbackToContractType() {
		assertThat(settingsProvider.getCostCenter(new Contract().type(LAND_LEASE_PUBLIC))).isEqualTo(COST_CENTER_36000000);
	}

	@Test
	void getDepartmentFallbackToContractType() {
		assertThat(settingsProvider.getDepartment(new Contract().type(LAND_LEASE_PUBLIC))).isEqualTo(DEPARTMENT_810100);
	}

	@Test
	void getSubaccountFallbackToContractType() {
		assertThat(settingsProvider.getSubaccount(new Contract().type(LAND_LEASE_PUBLIC))).isEqualTo(SUB_ACCOUNT_342000);
	}

	@Test
	void getVatCodeFallbackToContractType() {
		assertThat(settingsProvider.getVatCode(new Contract().type(LAND_LEASE_PUBLIC))).isEqualTo(VATCODE_00);
	}
}
