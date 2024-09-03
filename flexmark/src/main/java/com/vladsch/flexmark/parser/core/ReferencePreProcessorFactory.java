package com.vladsch.flexmark.parser.core;

import com.vladsch.flexmark.parser.block.ParagraphPreProcessor;
import com.vladsch.flexmark.parser.block.ParagraphPreProcessorFactory;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.parser.internal.InlineParserImpl;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

public class ReferencePreProcessorFactory implements ParagraphPreProcessorFactory {
  @Override
  public boolean affectsGlobalScope() {
    return true;
  }

  @Nullable
  @Override
  public Set<Class<?>> getAfterDependents() {
    return null;
  }

  @Nullable
  @Override
  public Set<Class<?>> getBeforeDependents() {
    return null;
  }

  @Override
  public ParagraphPreProcessor apply(ParserState state) {
    return ((InlineParserImpl) state.getInlineParser());
  }
}
