package com.vladsch.flexmark.util.ast;

import com.vladsch.flexmark.util.misc.BitFieldSet;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.builder.ISequenceBuilder;

public interface TextContainer {
  int F_LINK_TEXT_TYPE = BitFieldSet.intMask(Flags.LINK_TEXT_TYPE);
  int F_LINK_TEXT = 0; // use link text
  int F_LINK_PAGE_REF = 1; // use page ref
  int F_LINK_ANCHOR = 2; // use link anchor
  int F_LINK_URL = 3; // use link URL
  int F_LINK_NODE_TEXT = 4; // use node text

  int F_NODE_TEXT = BitFieldSet.intMask(Flags.NODE_TEXT);
  int F_FOR_HEADING_ID = BitFieldSet.intMask(Flags.FOR_HEADING_ID);
  int F_NO_TRIM_REF_TEXT_START = BitFieldSet.intMask(Flags.NO_TRIM_REF_TEXT_START);
  int F_NO_TRIM_REF_TEXT_END = BitFieldSet.intMask(Flags.NO_TRIM_REF_TEXT_END);
  int F_ADD_SPACES_BETWEEN_NODES = BitFieldSet.intMask(Flags.ADD_SPACES_BETWEEN_NODES);

  /**
   * Append node's text
   *
   * @param out sequence build to which to append text
   * @param flags collection flags
   * @param nodeVisitor node visitor to use to visit children
   * @return true if child nodes should be visited
   */
  boolean collectText(
      ISequenceBuilder<? extends ISequenceBuilder<?, BasedSequence>, BasedSequence> out,
      int flags,
      NodeVisitor nodeVisitor);

  /**
   * Append node's text ending, after any child nodes have been visited. The default implementation
   * does nothing.
   *
   * @param out sequence build to which to append text
   * @param flags collection flags
   * @param nodeVisitor node visitor to use to visit children
   */
  default void collectEndText(
      ISequenceBuilder<? extends ISequenceBuilder<?, BasedSequence>, BasedSequence> out,
      int flags,
      NodeVisitor nodeVisitor) {}
}
