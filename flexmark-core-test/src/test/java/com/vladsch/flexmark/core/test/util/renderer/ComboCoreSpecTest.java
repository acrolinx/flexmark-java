package com.vladsch.flexmark.core.test.util.renderer;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.test.util.TestUtils;
import com.vladsch.flexmark.test.util.spec.ResourceLocation;
import com.vladsch.flexmark.test.util.spec.SpecExample;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.runners.Parameterized;

public final class ComboCoreSpecTest extends CoreRendererSpecTest {
  static final String SPEC_RESOURCE = "/ast_spec.md";
  public static final @NotNull ResourceLocation RESOURCE_LOCATION =
      ResourceLocation.of(SPEC_RESOURCE);

  private static final DataHolder OPTIONS =
      new MutableDataSet()
          .set(HtmlRenderer.INDENT_SIZE, 0)
          .set(Parser.INLINE_DELIMITER_DIRECTIONAL_PUNCTUATIONS, false)
          .set(HtmlRenderer.PERCENT_ENCODE_URLS, true)
          .set(TestUtils.NO_FILE_EOL, false)
          .toImmutable();

  public ComboCoreSpecTest(@NotNull SpecExample example) {
    super(example, null, OPTIONS);
  }

  protected boolean compoundSections() {
    return false;
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> data() {
    return getTestData(RESOURCE_LOCATION);
  }
}
