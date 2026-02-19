package se.sundsvall.billingdatacollector.integration.opene.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class XPathExceptionTests {

	@Test
	void constructorWithMessage() {
		var exception = new XPathException("someMessage");

		assertThat(exception.getMessage()).isEqualTo("someMessage");
	}

	@Test
	void constructorWithMessageAndCause() {
		var exception = new XPathException("someMessage", new RuntimeException());

		assertThat(exception.getMessage()).isEqualTo("someMessage");
		assertThat(exception.getCause()).isInstanceOf(RuntimeException.class);
	}
}
