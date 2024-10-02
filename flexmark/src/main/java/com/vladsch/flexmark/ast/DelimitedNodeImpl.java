package com.vladsch.flexmark.ast;

import com.vladsch.flexmark.util.ast.DelimitedNode;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

public abstract class DelimitedNodeImpl extends Node implements DelimitedNode {
  protected BasedSequence openingMarker = BasedSequence.NULL;
  protected BasedSequence text = BasedSequence.NULL;
  protected BasedSequence closingMarker = BasedSequence.NULL;

  @Override
  public void getAstExtra(@NotNull StringBuilder out) {
    delimitedSegmentSpanChars(out, openingMarker, text, closingMarker, "text");
  }

  @NotNull
  @Override
  public BasedSequence[] getSegments() {
    return new BasedSequence[] {openingMarker, text, closingMarker};
  }

  public DelimitedNodeImpl() {}

  public DelimitedNodeImpl(BasedSequence chars) {
    super(chars);
  }

  public DelimitedNodeImpl(
      BasedSequence openingMarker, BasedSequence text, BasedSequence closingMarker) {
    super(
        openingMarker.baseSubSequence(
            openingMarker.getStartOffset(), closingMarker.getEndOffset()));

    this.openingMarker = openingMarker;
    this.text = text;
    this.closingMarker = closingMarker;
  }

  @Override
  public BasedSequence getOpeningMarker() {
    return openingMarker;
  }

  @Override
  public void setOpeningMarker(BasedSequence openingMarker) {
    this.openingMarker = openingMarker;
  }

  @Override
  public BasedSequence getText() {
    return text;
  }

  @Override
  public void setText(BasedSequence text) {
    this.text = text;
  }

  @Override
  public BasedSequence getClosingMarker() {
    return closingMarker;
  }

  @Override
  public void setClosingMarker(BasedSequence closingMarker) {
    this.closingMarker = closingMarker;
  }
}
