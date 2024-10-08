package com.vladsch.flexmark.util.ast;

import static com.vladsch.flexmark.util.misc.BitFieldSet.any;

import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.builder.ISequenceBuilder;

public interface DelimitedNode extends TextContainer {
  BasedSequence getOpeningMarker();

  BasedSequence getChars();

  void setOpeningMarker(BasedSequence openingMarker);

  BasedSequence getText();

  void setText(BasedSequence text);

  BasedSequence getClosingMarker();

  void setClosingMarker(BasedSequence closingMarker);

  @Override
  default boolean collectText(
      ISequenceBuilder<? extends ISequenceBuilder<?, BasedSequence>, BasedSequence> out,
      int flags,
      NodeVisitor nodeVisitor) {
    if (any(flags, F_NODE_TEXT)) {
      out.append(getText());
      return false;
    }

    return true;
  }
}
