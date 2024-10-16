package com.vladsch.flexmark.ast;

import com.vladsch.flexmark.util.sequence.BasedSequence;
import org.jetbrains.annotations.NotNull;

public abstract class InlineLinkNode extends LinkNode {
  protected BasedSequence textOpeningMarker = BasedSequence.NULL;
  protected BasedSequence text = BasedSequence.NULL;
  protected BasedSequence textClosingMarker = BasedSequence.NULL;
  protected BasedSequence linkOpeningMarker = BasedSequence.NULL;
  protected BasedSequence linkClosingMarker = BasedSequence.NULL;

  @NotNull
  @Override
  public BasedSequence[] getSegments() {
    return new BasedSequence[] {
      textOpeningMarker,
      text,
      textClosingMarker,
      linkOpeningMarker,
      urlOpeningMarker,
      url,
      pageRef,
      anchorMarker,
      anchorRef,
      urlClosingMarker,
      titleOpeningMarker,
      title,
      titleClosingMarker,
      linkClosingMarker
    };
  }

  @NotNull
  @Override
  public BasedSequence[] getSegmentsForChars() {
    return new BasedSequence[] {
      textOpeningMarker,
      text,
      textClosingMarker,
      linkOpeningMarker,
      urlOpeningMarker,
      pageRef,
      anchorMarker,
      anchorRef,
      urlClosingMarker,
      titleOpeningMarker,
      title,
      titleClosingMarker,
      linkClosingMarker
    };
  }

  @Override
  public void getAstExtra(@NotNull StringBuilder out) {
    delimitedSegmentSpanChars(out, textOpeningMarker, text, textClosingMarker, "text");
    segmentSpanChars(out, linkOpeningMarker, "linkOpen");
    delimitedSegmentSpanChars(out, urlOpeningMarker, url, urlClosingMarker, "url");
    if (pageRef.isNotNull()) segmentSpanChars(out, pageRef, "pageRef");
    if (anchorMarker.isNotNull()) segmentSpanChars(out, anchorMarker, "anchorMarker");
    if (anchorRef.isNotNull()) segmentSpanChars(out, anchorRef, "anchorRef");
    delimitedSegmentSpanChars(out, titleOpeningMarker, title, titleClosingMarker, "title");
    segmentSpanChars(out, linkClosingMarker, "linkClose");
  }

  protected InlineLinkNode() {}

  public abstract void setTextChars(BasedSequence textChars);

  public BasedSequence getText() {
    return text;
  }

  public BasedSequence getTextOpeningMarker() {
    return textOpeningMarker;
  }

  public void setTextOpeningMarker(BasedSequence textOpeningMarker) {
    this.textOpeningMarker = textOpeningMarker;
  }

  public void setText(BasedSequence text) {
    this.text = text.trim();
  }

  public BasedSequence getTextClosingMarker() {
    return textClosingMarker;
  }

  public void setTextClosingMarker(BasedSequence textClosingMarker) {
    this.textClosingMarker = textClosingMarker;
  }

  public BasedSequence getLinkOpeningMarker() {
    return linkOpeningMarker;
  }

  public void setLinkOpeningMarker(BasedSequence linkOpeningMarker) {
    this.linkOpeningMarker = linkOpeningMarker;
  }

  public BasedSequence getLinkClosingMarker() {
    return linkClosingMarker;
  }

  public void setLinkClosingMarker(BasedSequence linkClosingMarker) {
    this.linkClosingMarker = linkClosingMarker;
  }

  @NotNull
  @Override
  protected String toStringAttributes() {
    return "text=" + text + ", url=" + url + ", title=" + title;
  }
}
