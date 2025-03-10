package com.vladsch.flexmark.ast;

import com.vladsch.flexmark.util.ast.DelimitedNode;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;

public abstract class DelimitedNodeImpl extends Node implements DelimitedNode {
  BasedSequence openingMarker = BasedSequence.NULL;
  BasedSequence text = BasedSequence.NULL;
  BasedSequence closingMarker = BasedSequence.NULL;

  @Override
  public void getAstExtra(StringBuilder out) {
    delimitedSegmentSpanChars(out, openingMarker, text, closingMarker, "text");
  }

  @Override
  public BasedSequence[] getSegments() {
    return new BasedSequence[] {openingMarker, text, closingMarker};
  }

  DelimitedNodeImpl() {}

  DelimitedNodeImpl(BasedSequence chars) {
    super(chars);
  }

  DelimitedNodeImpl(BasedSequence openingMarker, BasedSequence text, BasedSequence closingMarker) {
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
