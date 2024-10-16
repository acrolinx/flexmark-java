package com.vladsch.flexmark.test.util;

import com.vladsch.flexmark.test.util.spec.SpecExample;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class SpecExampleRendererBase implements SpecExampleRenderer {
  protected final @NotNull SpecExample myExample;
  protected final @NotNull DataHolder myOptions;
  protected final boolean myIncludeExampleInfo;
  private boolean myIsFinalized;
  private @Nullable String myRenderedHtml;
  private @Nullable String myRenderedAst;

  protected SpecExampleRendererBase(@NotNull SpecExample example, @Nullable DataHolder options) {
    this(example, options, true);
  }

  protected SpecExampleRendererBase(
      @NotNull SpecExample example, @Nullable DataHolder options, boolean includeExampleInfo) {
    myExample = example;
    myOptions = options == null ? new DataSet() : options.toImmutable();
    myIncludeExampleInfo = includeExampleInfo;
  }

  public boolean isFinalized() {
    return myIsFinalized;
  }

  @Override
  public final @NotNull String getHtml() {
    if (myRenderedHtml == null || !isFinalized()) {
      myRenderedHtml = renderHtml();
    }
    return myRenderedHtml;
  }

  @Override
  public final @NotNull String getAst() {
    if (myRenderedAst == null || !isFinalized()) {
      myRenderedAst = renderAst();
    }
    return myRenderedAst;
  }

  @NotNull
  protected abstract String renderHtml();

  @NotNull
  protected abstract String renderAst();

  @Override
  public void finalizeRender() {
    myIsFinalized = true;
  }

  @Override
  public boolean includeExampleInfo() {
    return myIncludeExampleInfo;
  }

  @Override
  @NotNull
  public SpecExample getExample() {
    return myExample;
  }

  @NotNull
  @Override
  public DataHolder getOptions() {
    return myOptions.toImmutable();
  }
}
