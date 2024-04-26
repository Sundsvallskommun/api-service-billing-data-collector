package se.sundsvall.billingdatacollector.integration.opene.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.jsoup.nodes.Element;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class XPathUtilTests {

    @Test
    void parseXmlDocument() {
        var xml = """
        <?xml version="1.0" encoding="UTF-8"?>
        <data>
            <name>SOME NAME</name>
            <items>
                <item>ITEM1</item>
                <item>ITEM2</item>
            </items>
        </data>
        """;

        var document = (Element) XPathUtil.parseXmlDocument(xml.getBytes(UTF_8));

        assertThat(document).isNotNull();
        assertThat(document.children()).hasSize(2);
    }

    @Nested
    class ParameterTests {

        private Object dummy;

        @Test
        void constructorAndAccessors() throws Exception {
            var field = getClass().getDeclaredField("dummy");
            var type = getClass();
            var value = "someValue";

            var parameter = new XPathUtil.Parameter(field, type, value);

            assertThat(parameter.field()).isEqualTo(field);
            assertThat(parameter.type()).isEqualTo(type);
            assertThat(parameter.value()).isEqualTo(value);
        }
    }
}
