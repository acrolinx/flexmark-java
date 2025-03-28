package com.vladsch.flexmark.ast;

import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.BlockQuoteLike;
import com.vladsch.flexmark.util.ast.KeepTrailingBlankLineContainer;
import com.vladsch.flexmark.util.sequence.BasedSequence;

public class BlockQuote extends Block implements BlockQuoteLike, KeepTrailingBlankLineContainer {
  private BasedSequence openingMarker = BasedSequence.NULL;

  @Override
  public void getAstExtra(StringBuilder out) {
    segmentSpanChars(out, openingMarker, "marker");
  }

  @Override
  public BasedSequence[] getSegments() {
    return new BasedSequence[] {openingMarker};
  }

  public BlockQuote() {}

  @Override
  public BasedSequence getOpeningMarker() {
    return openingMarker;
  }

  public void setOpeningMarker(BasedSequence openingMarker) {
    this.openingMarker = openingMarker;
  }
}
