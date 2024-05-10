package se.sundsvall.billingdatacollector.integration.opene;

import static java.util.stream.Collectors.toMap;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.evaluateXPath;
import static se.sundsvall.billingdatacollector.integration.opene.util.XPathUtil.getString;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.zalando.problem.Problem;
import org.zalando.problem.Status;

import se.sundsvall.billingdatacollector.model.BillingRecordWrapper;

@Component
public class OpenEIntegration {

	private static final Logger LOG = LoggerFactory.getLogger(OpenEIntegration.class);

	private final OpenEClient client;
	private final Map<String, OpenEMapper> mappers;

	OpenEIntegration(final OpenEClient client, final List<OpenEMapper> mappers) {
		this.client = client;
		this.mappers = mappers.stream().collect(toMap(OpenEMapper::getSupportedFamilyId, Function.identity()));
	}

	public List<String> getErrandIds(final String familyId, final String fromDate, final String toDate) {
		// Get the XML from OpenE...
		var xml = client.getErrands(familyId, fromDate, toDate);
		// Extract the errand id:s
		var result = evaluateXPath(xml, "/FlowInstances/FlowInstance/flowInstanceID");

		return result.eachText().stream()
			.map(String::trim)
			.toList();
	}

	public BillingRecordWrapper getBillingRecord(final String flowInstanceId) {
		// Get the XML from OpenE...
		var xml = client.getErrand(flowInstanceId);

		// Extract the familyId
		var familyId = getString(xml, "/FlowInstance/Header/Flow/FamilyID");
		// Bail out if there is no mapper to handle the given familyId
		if (!mappers.containsKey(familyId)) {
			throw Problem.valueOf(Status.INTERNAL_SERVER_ERROR, "No mapper for familyId " + familyId);
		}

		var billingRecordWrapper = mappers.get(familyId).mapToBillingRecord(xml);
		//Set the familyId to make it possible to apply decorator
		billingRecordWrapper.setFamilyId(familyId);

		return billingRecordWrapper;
	}

}
