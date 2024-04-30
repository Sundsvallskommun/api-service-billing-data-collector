package se.sundsvall.billingdatacollector.integration.opene.util;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import se.sundsvall.billingdatacollector.integration.opene.util.annotation.XPath;
import se.sundsvall.dept44.test.annotation.resource.Load;
import se.sundsvall.dept44.test.extension.ResourceLoaderExtension;

@ExtendWith({MockitoExtension.class, ResourceLoaderExtension.class})
class XPathUtilTests {

    private byte[] xml;

    @BeforeEach
    void setUp(@Load("/open-e/misc.xml") final String xml) {
        this.xml = xml.getBytes(UTF_8);
    }

    @Test
    void extractValueThrowsExceptionForAbstractClasses() {
        abstract class DummyClass {

        }

        assertThatExceptionOfType(XPathException.class)
            .isThrownBy(() -> XPathUtil.extractValue(xml, DummyClass.class))
            .withMessageEndingWith("must be a concrete class or a record");
    }

    @Test
    void extractValueThrowsExceptionForInterfaces() {
        interface DummyInterface {

        }

        assertThatExceptionOfType(XPathException.class)
            .isThrownBy(() -> XPathUtil.extractValue(xml, DummyInterface.class))
            .withMessageEndingWith("must be a concrete class or a record");
    }

    @Test
    void extractValueForClass() {
        var result = XPathUtil.extractValue(xml, DishAsClass.class);

        assertThat(result).isNotNull().satisfies(dish -> assertThat(dish.name).isEqualTo("Waffles"));
    }

    @Test
    void extractValueForRecord() {
        var result = XPathUtil.extractValue(xml, DishAsRecord.class);

        assertThat(result).isNotNull().satisfies(dish -> assertThat(dish.name).isEqualTo("Cheeseburger"));
    }

    @Test
    void getValueForString() {
        var dummyStringValue = "someStringValue";

        try (var mockXPathUtil = mockStatic(XPathUtil.class)) {
            mockXPathUtil.when(() -> XPathUtil.getValue(any(), any(String.class), any())).thenCallRealMethod();

            mockXPathUtil.when(() -> XPathUtil.getString(xml, "/some/path")).thenReturn(dummyStringValue);

            assertThat(XPathUtil.getValue(xml, "/some/path", String.class)).isEqualTo(dummyStringValue);

            mockXPathUtil.verify(() -> XPathUtil.getValue(any(), any(String.class), any()));
            mockXPathUtil.verify(() -> XPathUtil.getString(xml, "/some/path"));
            mockXPathUtil.verifyNoMoreInteractions();
        }
    }

    @Test
    void getValueForInteger() {
        var dummyIntegerValue = 12345;

        try (var mockXPathUtil = mockStatic(XPathUtil.class)) {
            mockXPathUtil.when(() -> XPathUtil.getValue(any(), any(String.class), any())).thenCallRealMethod();
            mockXPathUtil.when(() -> XPathUtil.getInteger(xml, "/some/path")).thenReturn(dummyIntegerValue);

            assertThat(XPathUtil.getValue(xml, "/some/path", Integer.class)).isEqualTo(dummyIntegerValue);

            mockXPathUtil.verify(() -> XPathUtil.getValue(any(), any(String.class), any()));
            mockXPathUtil.verify(() -> XPathUtil.getInteger(xml, "/some/path"));
            mockXPathUtil.verifyNoMoreInteractions();
        }
    }

    @Test
    void getValueForBoolean() {
        var dummyBooleanValue = true;

        try (var mockXPathUtil = mockStatic(XPathUtil.class)) {
            mockXPathUtil.when(() -> XPathUtil.getValue(any(), any(String.class), any())).thenCallRealMethod();
            mockXPathUtil.when(() -> XPathUtil.getBoolean(xml, "/some/path")).thenReturn(dummyBooleanValue);

            assertThat(XPathUtil.getValue(xml, "/some/path", Boolean.class)).isEqualTo(dummyBooleanValue);

            mockXPathUtil.verify(() -> XPathUtil.getValue(any(), any(String.class), any()));
            mockXPathUtil.verify(() -> XPathUtil.getBoolean(xml, "/some/path"));
            mockXPathUtil.verifyNoMoreInteractions();
        }
    }

    @Test
    void getValueForDouble() {
        var dummyDoubleValue = 123.45;

        try (var mockXPathUtil = mockStatic(XPathUtil.class)) {
            mockXPathUtil.when(() -> XPathUtil.getValue(any(), any(String.class), any())).thenCallRealMethod();
            mockXPathUtil.when(() -> XPathUtil.getDouble(xml, "/some/path")).thenReturn(dummyDoubleValue);

            assertThat(XPathUtil.getValue(xml, "/some/path", Double.class)).isEqualTo(dummyDoubleValue);

            mockXPathUtil.verify(() -> XPathUtil.getValue(any(), any(String.class), any()));
            mockXPathUtil.verify(() -> XPathUtil.getDouble(xml, "/some/path"));
            mockXPathUtil.verifyNoMoreInteractions();
        }
    }

    @Test
    void getValueForFloat() {
        var dummyFloatValue = 123.45f;

        try (var mockXPathUtil = mockStatic(XPathUtil.class)) {
            mockXPathUtil.when(() -> XPathUtil.getValue(any(), any(String.class), any())).thenCallRealMethod();
            mockXPathUtil.when(() -> XPathUtil.getFloat(xml, "/some/path")).thenReturn(dummyFloatValue);

            assertThat(XPathUtil.getValue(xml, "/some/path", Float.class)).isEqualTo(dummyFloatValue);

            mockXPathUtil.verify(() -> XPathUtil.getValue(any(), any(String.class), any()));
            mockXPathUtil.verify(() -> XPathUtil.getFloat(xml, "/some/path"));
            mockXPathUtil.verifyNoMoreInteractions();
        }
    }

    @Test
    void getValueForOtherType() {
        var dummyDish = new DishAsRecord("someDummyValue");

        try (var mockXPathUtil = mockStatic(XPathUtil.class)) {
            mockXPathUtil.when(() -> XPathUtil.getValue(any(), any(String.class), any())).thenCallRealMethod();
            mockXPathUtil.when(() -> XPathUtil.extractValue(xml, DishAsRecord.class)).thenReturn(dummyDish);

            assertThat(XPathUtil.getValue(xml, "/some/path", DishAsRecord.class)).isEqualTo(dummyDish);

            mockXPathUtil.verify(() -> XPathUtil.getValue(any(), any(String.class), any()));
            mockXPathUtil.verify(() -> XPathUtil.extractValue(xml, DishAsRecord.class));
            mockXPathUtil.verifyNoMoreInteractions();
        }
    }

    @Test
    void getString() {
        var first = XPathUtil.getString(xml, "/menu/dish[1]/name");
        var second = XPathUtil.getString(xml, "/menu/dish[2]/name");
        var third = XPathUtil.getString(xml, "/menu/dish[3]/name");

        assertThat(first).isEqualTo("Waffles");
        assertThat(second).isEqualTo("Cheeseburger");
        assertThat(third).isNull();
    }

    @Test
    void getInteger() {
        var first = XPathUtil.getInteger(xml, "/menu/dish[1]/calories");
        var second = XPathUtil.getInteger(xml, "/menu/dish[2]/calories");
        var third = XPathUtil.getInteger(xml, "/menu/dish[3]/calories");

        assertThat(first).isEqualTo(650);
        assertThat(second).isEqualTo(900);
        assertThat(third).isNull();
    }

    @Test
    void getBoolean() {
        var first = XPathUtil.getBoolean(xml, "/menu/dish[1]/vegetarian");
        var second = XPathUtil.getBoolean(xml, "/menu/dish[2]/vegetarian");
        var third = XPathUtil.getBoolean(xml, "/menu/dish[3]/vegetarian");

        assertThat(first).isTrue();
        assertThat(second).isFalse();
        assertThat(third).isNull();
    }

    @Test
    void getDouble() {
        var first = XPathUtil.getDouble(xml, "/menu/dish[1]/price");
        var second = XPathUtil.getDouble(xml, "/menu/dish[2]/price");
        var third = XPathUtil.getDouble(xml, "/menu/dish[3]/price");

        assertThat(first).isEqualTo(55.0);
        assertThat(second).isEqualTo(119.95);
        assertThat(third).isNull();
    }

    @Test
    void getFloat() {
        var first = XPathUtil.getFloat(xml, "/menu/dish[1]/price");
        var second = XPathUtil.getFloat(xml, "/menu/dish[2]/price");
        var third = XPathUtil.getFloat(xml, "/menu/dish[3]/price");

        assertThat(first).isEqualTo(55.0f);
        assertThat(second).isEqualTo(119.95f);
        assertThat(third).isNull();
    }

    static class DishAsClass {

        @XPath("/menu/dish[1]/name")
        private String name;

        private String something;
    }

    record DishAsRecord(@XPath("/menu/dish[2]/name") String name) { }

    @Nested
    class ParameterTests {

        @SuppressWarnings("unused") // Suppressed since it's used only for this test
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
