package com.vladsch.flexmark.ext.gfm.strikethrough;

import com.vladsch.flexmark.core.test.util.RendererSpecTest;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.test.util.spec.ResourceLocation;
import com.vladsch.flexmark.test.util.spec.SpecExample;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.junit.runners.Parameterized;

public class ComboStrikethroughSubscriptSpecTest extends RendererSpecTest {
  static final String SPEC_RESOURCE = "/ext_strikethrough_subscript_ast_spec.md";
  public static final @NotNull ResourceLocation RESOURCE_LOCATION =
      ResourceLocation.of(SPEC_RESOURCE);
  private static final DataHolder OPTIONS =
      new MutableDataSet()
          .set(HtmlRenderer.INDENT_SIZE, 0)
          .set(Parser.EXTENSIONS, Collections.singleton(StrikethroughSubscriptExtension.create()))
          .toImmutable();

  private static final Map<String, DataHolder> optionsMap = new HashMap<>();

  static {
    optionsMap.put(
        "style-strikethrough",
        new MutableDataSet()
            .set(
                StrikethroughSubscriptExtension.STRIKETHROUGH_STYLE_HTML_OPEN,
                "<span class=\"text-strike\">")
            .set(StrikethroughSubscriptExtension.STRIKETHROUGH_STYLE_HTML_CLOSE, "</span>"));
    optionsMap.put(
        "style-subscript",
        new MutableDataSet()
            .set(
                StrikethroughSubscriptExtension.SUBSCRIPT_STYLE_HTML_OPEN,
                "<span class=\"text-sub\">")
            .set(StrikethroughSubscriptExtension.SUBSCRIPT_STYLE_HTML_CLOSE, "</span>"));
  }

  public ComboStrikethroughSubscriptSpecTest(@NotNull SpecExample example) {
    super(example, optionsMap, OPTIONS);
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> data() {
    return getTestData(RESOURCE_LOCATION);
  }
}
