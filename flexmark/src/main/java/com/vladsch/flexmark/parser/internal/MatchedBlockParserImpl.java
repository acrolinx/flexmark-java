package com.vladsch.flexmark.parser.internal;

import com.vladsch.flexmark.parser.block.BlockParser;
import com.vladsch.flexmark.parser.block.MatchedBlockParser;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import java.util.List;

class MatchedBlockParserImpl implements MatchedBlockParser {
  private final BlockParser matchedBlockParser;

  MatchedBlockParserImpl(BlockParser matchedBlockParser) {
    this.matchedBlockParser = matchedBlockParser;
  }

  @Override
  public List<BasedSequence> getParagraphLines() {
    if (matchedBlockParser.isParagraphParser()) {
      return matchedBlockParser.getBlockContent().getLines();
    }
    return null;
  }

  @Override
  public List<Integer> getParagraphEolLengths() {
    if (matchedBlockParser.isParagraphParser()) {
      return matchedBlockParser.getBlockContent().getLineIndents();
    }
    return null;
  }

  @Override
  public BlockParser getBlockParser() {
    return matchedBlockParser;
  }

  @Override
  public BasedSequence getParagraphContent() {
    if (matchedBlockParser.isParagraphParser()) {
      return matchedBlockParser.getBlockContent().getContents();
    }
    return null;
  }

  @Override
  public MutableDataHolder getParagraphDataHolder() {
    if (matchedBlockParser.isParagraphParser()) {
      return matchedBlockParser.getDataHolder();
    }
    return null;
  }
}
