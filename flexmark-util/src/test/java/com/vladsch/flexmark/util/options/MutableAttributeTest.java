package com.vladsch.flexmark.util.options;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import com.vladsch.flexmark.util.html.MutableAttribute;
import com.vladsch.flexmark.util.html.MutableAttributeImpl;
import org.junit.Test;

public class MutableAttributeTest {
  @Test
  public void testBasic() {
    MutableAttribute attribute = MutableAttributeImpl.of("name", "value1", ' ');
    assertEquals("no name change", "name", attribute.getName());

    assertTrue("contains a simple value", attribute.containsValue("value1"));

    MutableAttribute attribute1 = attribute.copy().setValue("value2");
    assertEquals("add a new value", "value1 value2", attribute1.getValue());
    assertNotEquals(attribute1, attribute);
    assertEquals("no name change", "name", attribute1.getName());

    MutableAttribute attribute2 = attribute.copy().removeValue("value2");
    assertEquals("remove non-existent value", "value1", attribute.getValue());
    assertEquals("remove non-existent value, no new attribute", attribute2, attribute);
    assertEquals(attribute2, attribute);
    assertEquals("no name change", "name", attribute2.getName());

    MutableAttribute attribute3 = attribute.copy().replaceValue("value2");
    assertEquals("replace value", "value2", attribute3.getValue());
    assertEquals("no name change", "name", attribute3.getName());

    MutableAttribute attribute4 = attribute1.setValue("value1");
    assertEquals("add existing value", "value1 value2", attribute4.getValue());
    assertEquals("add existing value, no new attribute", attribute4, attribute1);
    assertEquals("no name change", "name", attribute4.getName());

    MutableAttribute attribute5 = attribute1.setValue("value1");
    assertEquals("add existing value", "value1 value2", attribute5.getValue());
    assertEquals("add existing value, no new attribute", attribute5, attribute1);
    assertEquals("no name change", "name", attribute5.getName());

    MutableAttribute attribute6 = attribute1.copy().setValue("value2");
    assertEquals("add existing value", "value1 value2", attribute6.getValue());
    assertEquals("add existing value, no new attribute", attribute6, attribute1);
    assertEquals("no name change", "name", attribute6.getName());

    MutableAttribute attribute7 = attribute1.copy().setValue("value3");
    assertEquals("add existing value", "value1 value2 value3", attribute7.getValue());
    assertEquals("no name change", "name", attribute7.getName());

    MutableAttribute attribute8 = attribute7.copy().removeValue("value2");
    assertEquals("remove middle value", "value1 value3", attribute8.getValue());
    assertNotEquals(attribute8, attribute7);
    assertEquals("no name change", "name", attribute8.getName());

    MutableAttribute attribute9 = attribute3.copy().replaceValue("value2");
    assertEquals("replace value", "value2", attribute9.getValue());
    assertEquals("replace same value, no new attribute", attribute9, attribute3);
    assertEquals("no name change", "name", attribute9.getName());
  }

  @Test
  public void test_Style() {
    MutableAttribute attribute = MutableAttributeImpl.of("style", "");

    attribute.setValue("color:#white");
    assertEquals("add value", "color:#white", attribute.getValue());

    attribute.setValue("background:#black");
    assertEquals("add value", "color:#white;background:#black", attribute.getValue());

    attribute.setValue("font-family:monospaced;color:#green");
    assertEquals(
        "add and change multiple values",
        "color:#green;background:#black;font-family:monospaced",
        attribute.getValue());

    attribute.setValue("font-family");
    assertEquals("remove value", "color:#green;background:#black", attribute.getValue());

    attribute.removeValue("color;background");
    assertEquals("remove values", "", attribute.getValue());
  }
}
