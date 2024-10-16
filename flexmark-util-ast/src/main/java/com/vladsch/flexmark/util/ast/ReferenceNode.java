package com.vladsch.flexmark.util.ast;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ReferenceNode<B extends Node, N extends Node> extends Comparable<B> {
  @Nullable
  N getReferencingNode(@NotNull Node node);
}
