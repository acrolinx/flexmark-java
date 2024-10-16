package com.vladsch.flexmark.util.ast;

import com.vladsch.flexmark.util.sequence.BasedSequence;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Block extends ContentNode {
  protected Block() {}

  protected Block(@NotNull BasedSequence chars) {
    super(chars);
  }

  protected Block(@NotNull BasedSequence chars, @NotNull List<BasedSequence> lineSegments) {
    super(chars, lineSegments);
  }

  protected Block(@NotNull List<BasedSequence> lineSegments) {
    super(lineSegments);
  }

  @Override
  @Nullable
  public Block getParent() {
    return (Block) super.getParent();
  }

  @Override
  protected void setParent(@Nullable Node parent) {
    if (parent != null && !(parent instanceof Block)) {
      throw new IllegalArgumentException("Parent of block must also be block (can not be inline)");
    }
    super.setParent(parent);
  }
}
