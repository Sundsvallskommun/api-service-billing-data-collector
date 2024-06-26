package se.sundsvall.billingdatacollector.integration.messaging;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("falloutreport")
public record FalloutMessageProperties(
	FalloutMailTemplate falloutMailTemplate,
	List<String> recipients,
	String sender,
	String senderName) {

	public record FalloutMailTemplate(
		String subject,
		String htmlPrefix,
		String bodyPrefix,
		String listPrefix,
		String listItem,
		String listSuffix,
		String bodySuffix,
		String htmlSuffix) {
	}
}
