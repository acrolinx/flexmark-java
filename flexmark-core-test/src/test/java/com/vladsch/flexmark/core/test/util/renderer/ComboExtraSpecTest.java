package com.vladsch.flexmark.core.test.util.renderer;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.test.util.spec.ResourceLocation;
import com.vladsch.flexmark.test.util.spec.SpecExample;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.MutableDataSet;
import java.util.List;
import org.junit.runners.Parameterized;

public final class ComboExtraSpecTest extends CoreRendererSpecTest {
  private static final String SPEC_RESOURCE = "/core_extra_ast_spec.md";
  private static final ResourceLocation RESOURCE_LOCATION = ResourceLocation.of(SPEC_RESOURCE);

  private static final DataHolder OPTIONS =
      new MutableDataSet().set(HtmlRenderer.PERCENT_ENCODE_URLS, true).toImmutable();

  public ComboExtraSpecTest(SpecExample example) {
    super(example, null, OPTIONS);
  }

  @Parameterized.Parameters(name = "{0}")
  public static List<Object[]> data() {
    return getTestData(RESOURCE_LOCATION);
  }
}
