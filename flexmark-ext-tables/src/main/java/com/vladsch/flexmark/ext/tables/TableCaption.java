package com.vladsch.flexmark.ext.tables;

import com.vladsch.flexmark.util.ast.DelimitedNode;
import com.vladsch.flexmark.util.ast.LineBreakNode;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.sequence.BasedSequence;

/** Table caption of a {@link TableBlock} containing inline nodes. */
public class TableCaption extends Node implements DelimitedNode, LineBreakNode {
  private BasedSequence openingMarker = BasedSequence.NULL;
  private BasedSequence text = BasedSequence.NULL;
  private BasedSequence closingMarker = BasedSequence.NULL;

  public TableCaption(
      BasedSequence openingMarker, BasedSequence text, BasedSequence closingMarker) {
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

  @Override
  public BasedSequence[] getSegments() {
    return new BasedSequence[] {openingMarker, text, closingMarker};
  }

  @Override
  public void getAstExtra(StringBuilder out) {
    delimitedSegmentSpanChars(out, openingMarker, text, closingMarker, "text");
  }
}
