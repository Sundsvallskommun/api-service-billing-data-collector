package se.sundsvall.billingdatacollector.integration.opene;

import static java.util.stream.Collectors.toMap;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.evaluateXPath;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.getString;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;
import se.sundsvall.billingdatacollector.service.FalloutService;

@Component
public class OpenEIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(OpenEIntegration.class);

	private final OpenEClient client;
	private final Map<String, OpenEMapper> mappers;
	private final FalloutService falloutService;

	OpenEIntegration(final OpenEClient client, final List<OpenEMapper> mappers, FalloutService falloutService) {
		this.client = client;
		this.mappers = mappers.stream().collect(toMap(OpenEMapper::getSupportedFamilyId, Function.identity()));
		this.falloutService = falloutService;
	}

	public Set<String> getSupportedFamilyIds() {
		return new HashSet<>(mappers.keySet());
	}

	public List<String> getFlowInstanceIds(final String familyId, final String fromDate, final String toDate) {
		// Get the XML from OpenE...
		var xml = client.getErrands(familyId, fromDate, toDate);
		// Extract the errand id:s
		var result = evaluateXPath(xml, "/FlowInstances/FlowInstance/flowInstanceID");

		return result.eachText().stream()
			.map(String::trim)
			.toList();
	}

	/**
	 * Get a billing record from OpenE.
	 * If the familyId is not supported or if the familyId is not found in the XML, a Problem will be thrown.
	 * If the mapping fails, the XML will be saved to the fallout table.
	 * @param flowInstanceId The flowInstanceId to get the billing record for
	 * @return a {@link BillingRecordWrapper} with the billing record
	 */
	public Optional<BillingRecordWrapper> getBillingRecord(final String flowInstanceId) {
		// Get the XML from OpenE...
		var xml = client.getErrand(flowInstanceId);

		// Validate and extract the familyId
		var familyId = validateResponseAndExtractFamilyId(xml);

		BillingRecordWrapper billingRecordWrapper = null;

		try {
			// If we got a sane response and a mapper for the familyId, map the XML to a BillingRecordWrapper
			billingRecordWrapper = mappers.get(familyId).mapToBillingRecordWrapper(xml);
			//Set the familyId to make it possible to apply decorator
			billingRecordWrapper.setFamilyId(familyId);
			billingRecordWrapper.setFlowInstanceId(flowInstanceId);
		} catch (Exception e) {
			//If it fails, save it so we can investigate why.
			LOG.warn("Failed to map XML to BillingRecordWrapper, saving to fallout table", e);
			falloutService.saveFailedOpenEInstance(xml, flowInstanceId, familyId, e.getMessage());
		}

		return Optional.ofNullable(billingRecordWrapper);
	}

	private String validateResponseAndExtractFamilyId(byte[] xml) {
		// Extract the familyId
		var familyId = getString(xml, "/FlowInstance/Header/Flow/FamilyID");

		//If no familyId is found, throw a Problem
		Optional.ofNullable(familyId)
			.filter(StringUtils::isNotBlank)
			.orElseThrow(() -> Problem.builder()
				.withTitle("Couldn't map billing record from OpenE")
				.withDetail("No familyId found in response")
				.withStatus(Status.INTERNAL_SERVER_ERROR)
				.build());

		//If the familyId is not supported, also throw a Problem
		Optional.of(familyId)
			.filter(mappers::containsKey)
			.orElseThrow(() -> Problem.builder()
				.withTitle("Couldn't map billing record from OpenE")
				.withDetail("Unsupported familyId: " + familyId)
				.withStatus(Status.INTERNAL_SERVER_ERROR)
				.build());

		return familyId;
	}
}
