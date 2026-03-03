package se.sundsvall.billingdatacollector.service.source.contract;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.ContractType;
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
import static generated.se.sundsvall.contract.ContractType.LEASEHOLD;
import static generated.se.sundsvall.contract.ContractType.OBJECT_LEASE;
import static generated.se.sundsvall.contract.LeaseType.LAND_LEASE_MISC;
import static generated.se.sundsvall.contract.LeaseType.LAND_LEASE_RESIDENTIAL;
import static generated.se.sundsvall.contract.LeaseType.OTHER_FEE;
import static generated.se.sundsvall.contract.LeaseType.SITE_LEASE_COMMERCIAL;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_FARMING;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_HUNTING;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_MISC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;

@SpringBootTest(classes = SettingsProvider.class, webEnvironment = MOCK)
class SettingsProviderTest {
	private static final String ACTIVITY_3091 = "3091";
	private static final String ACTIVITY_3092 = "3092";
	private static final String ACTIVITY_3093 = "3093";
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
	void getActivity(LeaseType leaseType, ContractType contractType, String expectedValue) {
		assertThat(settingsProvider.getActivity(new Contract().leaseType(leaseType).type(contractType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getActivityArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, null, ACTIVITY_3091),
			Arguments.of(null, LAND_LEASE_PUBLIC, ACTIVITY_3093),
			Arguments.of(LAND_LEASE_RESIDENTIAL, null, ACTIVITY_3091),
			Arguments.of(null, LEASEHOLD, ACTIVITY_3092),
			Arguments.of(null, OBJECT_LEASE, ACTIVITY_3091),
			Arguments.of(OTHER_FEE, null, ACTIVITY_3091),
			Arguments.of(SITE_LEASE_COMMERCIAL, null, ACTIVITY_3091),
			Arguments.of(USUFRUCT_FARMING, null, ACTIVITY_3091),
			Arguments.of(USUFRUCT_HUNTING, null, ACTIVITY_3091),
			Arguments.of(USUFRUCT_MISC, null, ACTIVITY_3091),
			Arguments.of(null, null, null));
	}

	@ParameterizedTest
	@MethodSource("getCostCenterArgumentProvider")
	void getCostCenter(LeaseType leaseType, ContractType contractType, String expectedValue) {
		assertThat(settingsProvider.getCostCenter(new Contract().leaseType(leaseType).type(contractType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getCostCenterArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, null, COST_CENTER_36000000),
			Arguments.of(null, LAND_LEASE_PUBLIC, COST_CENTER_36000000),
			Arguments.of(LAND_LEASE_RESIDENTIAL, null, COST_CENTER_36000000),
			Arguments.of(null, LEASEHOLD, COST_CENTER_36000000),
			Arguments.of(null, OBJECT_LEASE, COST_CENTER_36000000),
			Arguments.of(OTHER_FEE, null, COST_CENTER_36000000),
			Arguments.of(SITE_LEASE_COMMERCIAL, null, COST_CENTER_36000000),
			Arguments.of(USUFRUCT_FARMING, null, COST_CENTER_36000000),
			Arguments.of(USUFRUCT_HUNTING, null, COST_CENTER_36000000),
			Arguments.of(USUFRUCT_MISC, null, COST_CENTER_36000000),
			Arguments.of(null, null, null));
	}

	@ParameterizedTest
	@MethodSource("getDepartmentArgumentProvider")
	void getDepartment(LeaseType leaseType, ContractType contractType, String expectedValue) {

		assertThat(settingsProvider.getDepartment(new Contract().leaseType(leaseType).type(contractType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getDepartmentArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, null, DEPARTMENT_810100),
			Arguments.of(null, LAND_LEASE_PUBLIC, DEPARTMENT_810100),
			Arguments.of(LAND_LEASE_RESIDENTIAL, null, DEPARTMENT_810100),
			Arguments.of(null, LEASEHOLD, DEPARTMENT_810100),
			Arguments.of(null, OBJECT_LEASE, DEPARTMENT_810100),
			Arguments.of(OTHER_FEE, null, DEPARTMENT_810100),
			Arguments.of(SITE_LEASE_COMMERCIAL, null, DEPARTMENT_810100),
			Arguments.of(USUFRUCT_FARMING, null, DEPARTMENT_810100),
			Arguments.of(USUFRUCT_HUNTING, null, DEPARTMENT_810100),
			Arguments.of(USUFRUCT_MISC, null, DEPARTMENT_810100),
			Arguments.of(null, null, null));
	}

	@ParameterizedTest
	@MethodSource("getSubaccountArgumentProvider")
	void getSubaccount(LeaseType leaseType, ContractType contractType, String expectedValue) {

		assertThat(settingsProvider.getSubaccount(new Contract().type(contractType).leaseType(leaseType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getSubaccountArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, null, SUB_ACCOUNT_342000),
			Arguments.of(null, LAND_LEASE_PUBLIC, SUB_ACCOUNT_342000),
			Arguments.of(LAND_LEASE_RESIDENTIAL, null, SUB_ACCOUNT_342000),
			Arguments.of(null, LEASEHOLD, SUB_ACCOUNT_342000),
			Arguments.of(null, OBJECT_LEASE, SUB_ACCOUNT_342000),
			Arguments.of(OTHER_FEE, null, SUB_ACCOUNT_342000),
			Arguments.of(SITE_LEASE_COMMERCIAL, null, SUB_ACCOUNT_342000),
			Arguments.of(USUFRUCT_FARMING, null, SUB_ACCOUNT_342100),
			Arguments.of(USUFRUCT_HUNTING, null, SUB_ACCOUNT_342100),
			Arguments.of(USUFRUCT_MISC, null, SUB_ACCOUNT_342000),
			Arguments.of(null, null, null));
	}

	@ParameterizedTest
	@MethodSource("getVatCodeArgumentProvider")
	void getVatCode(LeaseType leaseType, ContractType contractType, String expectedValue) {

		assertThat(settingsProvider.getVatCode(new Contract().leaseType(leaseType).type(contractType))).isEqualTo(expectedValue);
	}

	private static Stream<Arguments> getVatCodeArgumentProvider() {
		return Stream.of(
			Arguments.of(LAND_LEASE_MISC, null, VATCODE_00),
			Arguments.of(null, LAND_LEASE_PUBLIC, VATCODE_00),
			Arguments.of(LAND_LEASE_RESIDENTIAL, null, VATCODE_00),
			Arguments.of(null, LEASEHOLD, VATCODE_00),
			Arguments.of(null, OBJECT_LEASE, VATCODE_00),
			Arguments.of(OTHER_FEE, null, VATCODE_00),
			Arguments.of(SITE_LEASE_COMMERCIAL, null, VATCODE_00),
			Arguments.of(USUFRUCT_FARMING, null, VATCODE_25),
			Arguments.of(USUFRUCT_HUNTING, null, VATCODE_25),
			Arguments.of(USUFRUCT_MISC, null, VATCODE_00),
			Arguments.of(null, null, null));
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
