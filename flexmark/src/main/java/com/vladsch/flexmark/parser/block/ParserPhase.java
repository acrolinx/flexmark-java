package com.vladsch.flexmark.parser.block;

/**
 * Current Parser Phase as the document is parsed.
 *
 * <p><em>This enum is not visible by clients.</em>
 */
public enum ParserPhase {
  STARTING,
  PARSE_BLOCKS,
  PRE_PROCESS_PARAGRAPHS,
  PRE_PROCESS_BLOCKS,
  PARSE_INLINES,
  DONE,
}
