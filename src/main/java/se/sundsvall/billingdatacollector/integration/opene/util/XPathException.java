package se.sundsvall.billingdatacollector.integration.opene.util;

public class XPathException extends RuntimeException {

	private static final long serialVersionUID = 6572619945750488936L;

	public XPathException(final String message) {
		super(message);
	}

	public XPathException(final String message, final Throwable cause) {
		super(message, cause);
	}
}
