package com.vladsch.flexmark.ast;

import com.vladsch.flexmark.ast.util.ReferenceRepository;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.ast.DoNotLinkDecorate;
import com.vladsch.flexmark.util.ast.Document;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.ReferencingNode;
import com.vladsch.flexmark.util.ast.TextContainer;
import com.vladsch.flexmark.util.misc.BitFieldSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Escaping;
import com.vladsch.flexmark.util.sequence.ReplacedTextMapper;
import com.vladsch.flexmark.util.sequence.builder.ISequenceBuilder;

public abstract class RefNode extends Node
    implements LinkRefDerived,
        ReferencingNode<ReferenceRepository, Reference>,
        DoNotLinkDecorate,
        TextContainer {
  protected BasedSequence textOpeningMarker = BasedSequence.NULL;
  protected BasedSequence text = BasedSequence.NULL;
  protected BasedSequence textClosingMarker = BasedSequence.NULL;
  private BasedSequence referenceOpeningMarker = BasedSequence.NULL;
  private BasedSequence reference = BasedSequence.NULL;
  private BasedSequence referenceClosingMarker = BasedSequence.NULL;
  private boolean isDefined = false;

  @Override
  public BasedSequence[] getSegments() {
    if (isReferenceTextCombined()) {
      return new BasedSequence[] {
        referenceOpeningMarker,
        reference,
        referenceClosingMarker,
        textOpeningMarker,
        text,
        textClosingMarker,
      };
    }

    return new BasedSequence[] {
      textOpeningMarker,
      text,
      textClosingMarker,
      referenceOpeningMarker,
      reference,
      referenceClosingMarker,
    };
  }

  @Override
  public void getAstExtra(StringBuilder out) {
    if (isReferenceTextCombined()) {
      delimitedSegmentSpanChars(
          out, referenceOpeningMarker, reference, referenceClosingMarker, "reference");
      delimitedSegmentSpanChars(out, textOpeningMarker, text, textClosingMarker, "text");
    } else {
      delimitedSegmentSpanChars(out, textOpeningMarker, text, textClosingMarker, "text");
      delimitedSegmentSpanChars(
          out, referenceOpeningMarker, reference, referenceClosingMarker, "reference");
    }
  }

  protected RefNode() {}

  public void setReferenceChars(BasedSequence referenceChars) {
    int referenceCharsLength = referenceChars.length();
    int openingOffset = referenceChars.charAt(0) == '!' ? 2 : 1;
    this.referenceOpeningMarker = referenceChars.subSequence(0, openingOffset);
    this.reference = referenceChars.subSequence(openingOffset, referenceCharsLength - 1).trim();
    this.referenceClosingMarker =
        referenceChars.subSequence(referenceCharsLength - 1, referenceCharsLength);
  }

  public void setTextChars(BasedSequence textChars) {
    int textCharsLength = textChars.length();
    this.textOpeningMarker = textChars.subSequence(0, 1);
    this.text = textChars.subSequence(1, textCharsLength - 1).trim();
    this.textClosingMarker = textChars.subSequence(textCharsLength - 1, textCharsLength);
  }

  public boolean isReferenceTextCombined() {
    return text == BasedSequence.NULL;
  }

  @Override
  public boolean isDefined() {
    return isDefined;
  }

  public void setDefined(boolean defined) {
    isDefined = defined;
  }

  @Override
  public boolean isTentative() {
    return !isDefined;
  }

  public boolean isDummyReference() {
    return textOpeningMarker != BasedSequence.NULL
        && text == BasedSequence.NULL
        && textClosingMarker != BasedSequence.NULL;
  }

  public BasedSequence getText() {
    return text;
  }

  @Override
  public BasedSequence getReference() {
    return reference;
  }

  @Override
  public Reference getReferenceNode(Document document) {
    return getReferenceNode(Parser.REFERENCES.get(document));
  }

  @Override
  public Reference getReferenceNode(ReferenceRepository repository) {
    if (repository == null) {
      return null;
    }
    String normalizeRef = repository.normalizeKey(reference);
    return repository.get(normalizeRef);
  }

  public BasedSequence getTextOpeningMarker() {
    return textOpeningMarker;
  }

  public void setTextOpeningMarker(BasedSequence textOpeningMarker) {
    this.textOpeningMarker = textOpeningMarker;
  }

  public void setText(BasedSequence text) {
    this.text = text;
  }

  public BasedSequence getTextClosingMarker() {
    return textClosingMarker;
  }

  public void setTextClosingMarker(BasedSequence textClosingMarker) {
    this.textClosingMarker = textClosingMarker;
  }

  public BasedSequence getReferenceOpeningMarker() {
    return referenceOpeningMarker;
  }

  public void setReferenceOpeningMarker(BasedSequence referenceOpeningMarker) {
    this.referenceOpeningMarker = referenceOpeningMarker;
  }

  public void setReference(BasedSequence reference) {
    this.reference = reference;
  }

  public BasedSequence getDummyReference() {
    if (isDummyReference()) {
      return getChars()
          .baseSubSequence(textOpeningMarker.getStartOffset(), textClosingMarker.getEndOffset());
    }
    return BasedSequence.NULL;
  }

  public BasedSequence getReferenceClosingMarker() {
    return referenceClosingMarker;
  }

  public void setReferenceClosingMarker(BasedSequence referenceClosingMarker) {
    this.referenceClosingMarker = referenceClosingMarker;
  }

  @Override
  public boolean collectText(
      ISequenceBuilder<? extends ISequenceBuilder<?, BasedSequence>, BasedSequence> out,
      int flags,
      NodeVisitor nodeVisitor) {
    // images no longer add alt text

    int urlType = flags & F_LINK_TEXT_TYPE;

    if (urlType == F_LINK_TEXT) {
      // To allow using leading/trailing spaces for generating heading ids, need to include stripped
      // out spaces
      if (BitFieldSet.any(flags, F_FOR_HEADING_ID) && this instanceof ImageRef) {
        return false;
      }

      if (BitFieldSet.any(flags, F_FOR_HEADING_ID)
          && BitFieldSet.any(flags, F_NO_TRIM_REF_TEXT_START | F_NO_TRIM_REF_TEXT_END)) {
        BasedSequence[] segments = getSegments();

        if (BitFieldSet.any(flags, F_NO_TRIM_REF_TEXT_START)) {
          out.append(
              getChars().baseSubSequence(segments[0].getEndOffset(), segments[1].getStartOffset()));
        }

        nodeVisitor.visitChildren(this);

        if (BitFieldSet.any(flags, F_NO_TRIM_REF_TEXT_END)) {
          out.append(
              getChars().baseSubSequence(segments[1].getEndOffset(), segments[2].getStartOffset()));
        }

        return false;
      }

      return true;
    }

    Reference reference = getReferenceNode(getDocument());
    if (urlType == F_LINK_NODE_TEXT) {
      out.append(getChars());
    } else {
      if (reference == null) {
        return true;
      }

      BasedSequence url;

      switch (urlType) {
        case F_LINK_PAGE_REF:
          url = reference.getPageRef();
          break;

        case F_LINK_ANCHOR:
          url = reference.getAnchorRef();
          break;

        case F_LINK_URL:
          url = reference.getUrl();
          break;

        default:
          return true;
      }

      ReplacedTextMapper textMapper = new ReplacedTextMapper(url);
      BasedSequence unescaped = Escaping.unescape(url, textMapper);
      BasedSequence percentDecoded = Escaping.percentDecodeUrl(unescaped, textMapper);
      out.append(percentDecoded);
    }

    return false;
  }

  @Override
  protected String toStringAttributes() {
    return "text=" + text + ", reference=" + reference;
  }
}
