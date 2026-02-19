package se.sundsvall.billingdatacollector.integration.opene.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import se.sundsvall.billingdatacollector.integration.opene.kundfakturaformular.model.OpeneCollections;

import static java.util.Optional.ofNullable;

/**
 * Util class to parse XML from OpenE-"lists" into lists.
 * The XML contains no lists but instead each item in the list is a separate object suffixed an ID, e.g.
 * "BarakningarExtern1" to indicate that it is the first item in the list.
 * Each xml-object contains a number of properties (which may also be suffixed with an ID) that are mapped to fields in
 * a bean in the context.
 */
@Component
@ToString
public class ListUtil {

	private static final Logger LOGGER = LoggerFactory.getLogger(ListUtil.class);

	private static final String OEP_NAMESPACE_URI = "http://www.oeplatform.org/version/2.0/schemas/flowinstance";
	private static final String VALUES_NODE_NAME = "Values";
	private static final String ENDING_WITH_NUMBER_REGEX = "\\d$"; // Regex to match tags ending with a number
	private static final Pattern ENDING_WITH_NUMBER_PATTERN = Pattern.compile(ENDING_WITH_NUMBER_REGEX); // Pattern to match tags ending with a number

	private final ApplicationContext context;

	public ListUtil(ApplicationContext context) {
		this.context = context;
	}

	public OpeneCollections parseLists(byte[] xml) {
		LOGGER.info("Parsing xml from OpenE into lists");
		var openeCollections = new OpeneCollections();
		try {
			var nodeList = getNodeList(xml);

			if (nodeList.getLength() > 0) {
				var valuesNode = nodeList.item(0);

				// Get all child nodes of <Values> from the XML
				var childNodes = valuesNode.getChildNodes();

				// Iterate through the child nodes
				for (int childCounter = 0; childCounter < childNodes.getLength(); childCounter++) {
					var node = childNodes.item(childCounter);
					if (node.getNodeType() == Node.ELEMENT_NODE) {
						var nodeName = node.getLocalName(); // Get the node name without the namespace
						if (ENDING_WITH_NUMBER_PATTERN.matcher(nodeName).find()) { // Check if it ends with a number
							// Remove the number from the node name and make into camelCase
							// so we can match it against the bean name in the context
							addOpeneObject(nodeName, node, openeCollections);
						}
					}
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException e) {
			throw new IllegalStateException("Error parsing xml", e);
		}

		return openeCollections;
	}

	private void addOpeneObject(String nodeName, Node node, OpeneCollections openeCollections) {
		var id = nodeName.substring(nodeName.length() - 1);   // Get the id of the object, we will use this as the index in the map
		nodeName = nodeName.replaceAll(ENDING_WITH_NUMBER_REGEX, ""); // Remove the number from the node name so we can find the bean in the context
		final var nodeNameToUse = StringUtils.uncapitalize(nodeName); // Make sure the first letter is lowercase

		var beanToUse = getBean(nodeNameToUse);

		ofNullable(beanToUse).ifPresentOrElse(bean -> {
			var mappedBean = setProperties(bean, node.getChildNodes());
			openeCollections.add(Integer.parseInt(id), mappedBean);
		}, () -> LOGGER.warn("No bean found for xml-node: <{}>", nodeNameToUse));
	}

	private NodeList getNodeList(byte[] xml) throws ParserConfigurationException, SAXException, IOException {
		// Convert xml string to InputStream
		var inputStream = new ByteArrayInputStream(xml);

		var documentBuilderFactory = DocumentBuilderFactory.newDefaultInstance();
		documentBuilderFactory.setNamespaceAware(true); // Needed to parse namespaces
		var documentBuilder = documentBuilderFactory.newDocumentBuilder();
		var document = documentBuilder.parse(inputStream, StandardCharsets.ISO_8859_1.name());

		// Normalize the document
		document.getDocumentElement().normalize();

		// Check that we have the correct namespace, otherwise we won't be able to parse anything
		var rootNamespace = document.getDocumentElement().getNamespaceURI();
		if (!OEP_NAMESPACE_URI.equals(rootNamespace)) {
			throw new IllegalStateException("Namespace mismatch: expected " + OEP_NAMESPACE_URI + " but found " + rootNamespace);
		}

		// Get the <Values> node where all data we want is located
		return document.getElementsByTagNameNS(OEP_NAMESPACE_URI, VALUES_NODE_NAME);
	}

	private void setProperty(Object obj, String propertyName, String propertyValue) {
		try {
			// Capitalize the first letter of the property name and prepend "set"
			var setterName = "set" + StringUtils.capitalize(propertyName);

			// Remove any trailing numbers from the property name
			setterName = setterName.replaceAll(ENDING_WITH_NUMBER_REGEX, "");

			// Find the setter method
			var setterMethod = obj.getClass().getMethod(setterName, String.class);

			// Invoke the setter method
			setterMethod.invoke(obj, propertyValue);
		} catch (Exception e) {
			// No biggie
			LOGGER.warn("Couldn't set property {} on object {}", propertyName, obj.getClass().getName(), e);
		}
	}

	private <T> T setProperties(T object, NodeList nodeChildren) {
		for (int childCounter = 0; childCounter < nodeChildren.getLength(); childCounter++) {
			Node valueNode = nodeChildren.item(childCounter);
			if (valueNode.getNodeType() == Node.ELEMENT_NODE) {
				var nodeName = valueNode.getLocalName();
				var nodeValue = valueNode.getTextContent();

				setProperty(object, nodeName, nodeValue);
			}
		}

		return object;
	}

	private Object getBean(String name) {
		try {
			return context.getBean(name);
		} catch (BeansException e) {
			LOGGER.warn("No bean found for name: {}", name);
		}

		return null;
	}
}
