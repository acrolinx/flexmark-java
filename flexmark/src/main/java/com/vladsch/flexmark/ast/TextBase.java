package com.vladsch.flexmark.ast;

import static com.vladsch.flexmark.util.misc.BitFieldSet.any;

import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeVisitor;
import com.vladsch.flexmark.util.ast.TextContainer;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Escaping;
import com.vladsch.flexmark.util.sequence.ReplacedTextMapper;
import com.vladsch.flexmark.util.sequence.builder.ISequenceBuilder;

public class TextBase extends Node implements TextContainer {
  public TextBase() {}

  @Override
  public BasedSequence[] getSegments() {
    return EMPTY_SEGMENTS;
  }

  @Override
  public void getAstExtra(StringBuilder out) {
    astExtraChars(out);
  }

  @Override
  public boolean collectText(
      ISequenceBuilder<? extends ISequenceBuilder<?, BasedSequence>, BasedSequence> out,
      int flags,
      NodeVisitor nodeVisitor) {
    if (any(flags, F_NODE_TEXT)) {
      out.append(getChars());
    } else {
      ReplacedTextMapper textMapper = new ReplacedTextMapper(getChars());
      BasedSequence unescaped = Escaping.unescape(getChars(), textMapper);
      out.append(unescaped);
    }
    return false;
  }

  @Override
  protected String toStringAttributes() {
    return "text=" + getChars();
  }
}
