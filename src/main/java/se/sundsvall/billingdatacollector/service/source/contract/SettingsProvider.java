package se.sundsvall.billingdatacollector.service.source.contract;

import static generated.se.sundsvall.contract.LeaseType.LAND_LEASE_MISC;
import static generated.se.sundsvall.contract.LeaseType.LAND_LEASE_PUBLIC;
import static generated.se.sundsvall.contract.LeaseType.LAND_LEASE_RESIDENTIAL;
import static generated.se.sundsvall.contract.LeaseType.LEASEHOLD;
import static generated.se.sundsvall.contract.LeaseType.OBJECT_LEASE;
import static generated.se.sundsvall.contract.LeaseType.OTHER_FEE;
import static generated.se.sundsvall.contract.LeaseType.SITE_LEASE_COMMERCIAL;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_FARMING;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_HUNTING;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_MISC;
import static generated.se.sundsvall.contract.LeaseType.USUFRUCT_MOORING;
import static java.util.Optional.ofNullable;

import generated.se.sundsvall.contract.Contract;
import generated.se.sundsvall.contract.LeaseType;
import java.util.Map;
import org.springframework.stereotype.Component;
import se.sundsvall.billingdatacollector.service.source.contract.model.LeaseTypeSettings;

@Component
public class SettingsProvider {
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

	private final Map<LeaseType, LeaseTypeSettings> leaseTypeSettings;

	SettingsProvider() {
		leaseTypeSettings = Map.ofEntries(
			Map.entry(
				LAND_LEASE_PUBLIC, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3093)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342000)
					.withVatCode(VATCODE_00)
					.build()),
			Map.entry(
				SITE_LEASE_COMMERCIAL, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3091)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342000)
					.withVatCode(VATCODE_00)
					.build()),
			Map.entry(
				LAND_LEASE_RESIDENTIAL, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3091)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342000)
					.withVatCode(VATCODE_00)
					.build()),
			Map.entry(
				USUFRUCT_MOORING, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3099)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342000)
					.withVatCode(VATCODE_00)
					.build()),
			Map.entry(
				OBJECT_LEASE, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3091)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342000)
					.withVatCode(VATCODE_00)
					.build()),
			Map.entry(
				USUFRUCT_HUNTING, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3091)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342100)
					.withVatCode(VATCODE_25)
					.build()),
			Map.entry(
				USUFRUCT_FARMING, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3091)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342100)
					.withVatCode(VATCODE_25)
					.build()),
			Map.entry(
				LAND_LEASE_MISC, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3091)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342000)
					.withVatCode(VATCODE_00)
					.build()),
			Map.entry(
				USUFRUCT_MISC, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3091)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342000)
					.withVatCode(VATCODE_00)
					.build()),
			Map.entry(
				LEASEHOLD, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3092)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342000)
					.withVatCode(VATCODE_00)
					.build()),
			Map.entry(
				OTHER_FEE, LeaseTypeSettings.builder()
					.withActivity(ACTIVITY_3091)
					.withCostCenter(COST_CENTER_36000000)
					.withDepartment(DEPARTMENT_810100)
					.withSubAccount(SUB_ACCOUNT_342000)
					.withVatCode(VATCODE_00)
					.build()));
	}

	public boolean isLeaseTypeSettingsPresent(Contract contract) {
		return ofNullable(contract.getLeaseType())
			.map(leaseTypeSettings::containsKey)
			.orElse(false);
	}

	public String getActivity(Contract contract) {
		return ofNullable(contract.getLeaseType())
			.map(leaseTypeSettings::get)
			.map(LeaseTypeSettings::activity)
			.orElse(null);
	}

	public String getCostCenter(Contract contract) {
		return ofNullable(contract.getLeaseType())
			.map(leaseTypeSettings::get)
			.map(LeaseTypeSettings::costCenter)
			.orElse(null);
	}

	public String getDepartment(Contract contract) {
		return ofNullable(contract.getLeaseType())
			.map(leaseTypeSettings::get)
			.map(LeaseTypeSettings::department)
			.orElse(null);
	}

	public String getSubaccount(Contract contract) {
		return ofNullable(contract.getLeaseType())
			.map(leaseTypeSettings::get)
			.map(LeaseTypeSettings::subAccount)
			.orElse(null);
	}

	public String getVatCode(Contract contract) {
		return ofNullable(contract.getLeaseType())
			.map(leaseTypeSettings::get)
			.map(LeaseTypeSettings::vatCode)
			.orElse(null);
	}
}
