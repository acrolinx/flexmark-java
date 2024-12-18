package com.vladsch.flexmark.core.test.util.renderer;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.test.util.FlexmarkSpecExampleRenderer;
import com.vladsch.flexmark.test.util.FullSpecTestCase;
import com.vladsch.flexmark.test.util.SpecExampleRenderer;
import com.vladsch.flexmark.test.util.TestUtils;
import com.vladsch.flexmark.test.util.spec.SpecExample;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataSet;
import com.vladsch.flexmark.util.data.MutableDataSet;

public abstract class OrigSpecCoreTest extends FullSpecTestCase {
  private static final DataHolder OPTIONS =
      new MutableDataSet()
          .set(HtmlRenderer.PERCENT_ENCODE_URLS, true)
          .set(TestUtils.NO_FILE_EOL, false)
          .toImmutable();

  private final DataHolder myDefaultOptions;

  protected OrigSpecCoreTest(DataHolder defaultOptions) {
    myDefaultOptions = DataSet.aggregate(OPTIONS, defaultOptions);
  }

  @Override
  public final DataHolder options(String option) {
    return null;
  }

  @Override
  public final SpecExampleRenderer getSpecExampleRenderer(
      SpecExample example, DataHolder exampleOptions) {
    DataHolder combineOptions = DataSet.aggregate(myDefaultOptions, exampleOptions);
    return new FlexmarkSpecExampleRenderer(
        example,
        combineOptions,
        Parser.builder(combineOptions).build(),
        HtmlRenderer.builder(combineOptions).build(),
        false);
  }
}
