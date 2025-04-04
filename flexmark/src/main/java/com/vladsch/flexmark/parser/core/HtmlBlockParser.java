package com.vladsch.flexmark.parser.core;

import static com.vladsch.flexmark.parser.internal.HtmlDeepParser.HtmlMatch.COMMENT;
import static com.vladsch.flexmark.parser.internal.HtmlDeepParser.HtmlMatch.OPEN_TAG;

import com.vladsch.flexmark.ast.HtmlBlock;
import com.vladsch.flexmark.ast.HtmlBlockBase;
import com.vladsch.flexmark.ast.HtmlCommentBlock;
import com.vladsch.flexmark.ast.HtmlInnerBlock;
import com.vladsch.flexmark.ast.HtmlInnerBlockComment;
import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.util.Parsing;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.block.AbstractBlockParser;
import com.vladsch.flexmark.parser.block.AbstractBlockParserFactory;
import com.vladsch.flexmark.parser.block.BlockContinue;
import com.vladsch.flexmark.parser.block.BlockParser;
import com.vladsch.flexmark.parser.block.BlockParserFactory;
import com.vladsch.flexmark.parser.block.BlockStart;
import com.vladsch.flexmark.parser.block.CustomBlockParserFactory;
import com.vladsch.flexmark.parser.block.MatchedBlockParser;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.parser.internal.HtmlDeepParser;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.BlockContent;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlBlockParser extends AbstractBlockParser {
  private static final String HTML_COMMENT_OPEN = "<!--";
  private static final String HTML_COMMENT_CLOSE = "-->";

  private static class Patterns {
    public final int commentPatternIndex;
    public final Pattern[][] blockPatterns;

    public Patterns(Parsing parsing, DataHolder options) {
      this.commentPatternIndex = 2;

      // dynamic block tags
      StringBuilder sb = new StringBuilder();
      String delimiter = "";
      for (String tag : Parser.HTML_BLOCK_TAGS.get(options)) {
        sb.append(delimiter).append("\\Q").append(tag).append("\\E");
        delimiter = "|";
      }

      boolean forTranslator = Parser.HTML_FOR_TRANSLATOR.get(options);
      if (forTranslator) {
        sb.append(delimiter).append(Parser.TRANSLATION_HTML_BLOCK_TAG_PATTERN.get(options));
        delimiter = "|";
      }

      String blockTags = sb.toString();

      this.blockPatterns =
          new Pattern[][] {
            {null, null}, // not used (no type 0)
            {
              Pattern.compile("^<(?:script|pre|style)(?:\\s|>|$)", Pattern.CASE_INSENSITIVE),
              Pattern.compile("</(?:script|pre|style)>", Pattern.CASE_INSENSITIVE)
            },
            {Pattern.compile("^" + HTML_COMMENT_OPEN), Pattern.compile(HTML_COMMENT_CLOSE)},
            {Pattern.compile("^<[?]"), Pattern.compile("\\?>")},
            {Pattern.compile("^<![A-Z]"), Pattern.compile(">")},
            {Pattern.compile("^<!\\[CDATA\\["), Pattern.compile("\\]\\]>")},
            {
              Pattern.compile(
                  "^</?(?:" + Parsing.XML_NAMESPACE + "(?:" + blockTags + "))(?:\\s|[/]?[>]|$)",
                  Pattern.CASE_INSENSITIVE),
              null // terminated by blank line
            },
            {
              Pattern.compile(
                  "^(?:" + parsing.opentag + '|' + parsing.closetag + ")\\s*$",
                  Pattern.CASE_INSENSITIVE),
              null // terminated by blank line
            }
          };
    }
  }

  private final HtmlBlockBase block;
  private final Pattern closingPattern;
  private final HtmlDeepParser deepParser;

  private boolean finished = false;
  private BlockContent content = new BlockContent();
  private final boolean parseInnerHtmlComments;
  private final boolean myHtmlBlockDeepParseNonBlock;
  private final boolean myHtmlBlockDeepParseBlankLineInterrupts;
  private final boolean myHtmlBlockDeepParseMarkdownInterruptsClosed;
  private final boolean myHtmlBlockDeepParseBlankLineInterruptsPartialTag;
  private final boolean myHtmlBlockDeepParseIndentedCodeInterrupts;

  private HtmlBlockParser(
      DataHolder options, Pattern closingPattern, boolean isComment, HtmlDeepParser deepParser) {
    this.closingPattern = closingPattern;
    this.block = isComment ? new HtmlCommentBlock() : new HtmlBlock();
    this.deepParser = deepParser;
    this.parseInnerHtmlComments = Parser.PARSE_INNER_HTML_COMMENTS.get(options);
    this.myHtmlBlockDeepParseNonBlock = Parser.HTML_BLOCK_DEEP_PARSE_NON_BLOCK.get(options);
    this.myHtmlBlockDeepParseBlankLineInterrupts =
        Parser.HTML_BLOCK_DEEP_PARSE_BLANK_LINE_INTERRUPTS.get(options);
    this.myHtmlBlockDeepParseMarkdownInterruptsClosed =
        Parser.HTML_BLOCK_DEEP_PARSE_MARKDOWN_INTERRUPTS_CLOSED.get(options);
    this.myHtmlBlockDeepParseBlankLineInterruptsPartialTag =
        Parser.HTML_BLOCK_DEEP_PARSE_BLANK_LINE_INTERRUPTS_PARTIAL_TAG.get(options);
    this.myHtmlBlockDeepParseIndentedCodeInterrupts =
        Parser.HTML_BLOCK_DEEP_PARSE_INDENTED_CODE_INTERRUPTS.get(options);
  }

  @Override
  public Block getBlock() {
    return block;
  }

  @Override
  public BlockContinue tryContinue(ParserState state) {
    if (deepParser != null) {
      if (state.isBlank()) {
        if (deepParser.isHtmlClosed()
            || myHtmlBlockDeepParseBlankLineInterrupts && !deepParser.haveOpenRawTag()
            || (myHtmlBlockDeepParseBlankLineInterruptsPartialTag
                && deepParser.isBlankLineInterruptible())) {
          return BlockContinue.none();
        }
      }

      return BlockContinue.atIndex(state.getIndex());
    }

    if (finished) {
      return BlockContinue.none();
    }

    // Blank line ends type 6 and type 7 blocks
    if (state.isBlank() && closingPattern == null) {
      return BlockContinue.none();
    }

    return BlockContinue.atIndex(state.getIndex());
  }

  @Override
  public void addLine(ParserState state, BasedSequence line) {
    if (deepParser != null) {
      if (content.getLineCount() > 0) {
        // not the first line, which is already parsed
        deepParser.parseHtmlChunk(line, false, myHtmlBlockDeepParseNonBlock, false);
      }
    } else {
      if (closingPattern != null && closingPattern.matcher(line).find()) {
        finished = true;
      }
    }

    content.add(line, state.getIndent());
  }

  @Override
  public boolean canInterruptBy(BlockParserFactory blockParserFactory) {
    return myHtmlBlockDeepParseMarkdownInterruptsClosed
        && deepParser != null
        && !(blockParserFactory instanceof HtmlBlockParser.Factory
            || (!myHtmlBlockDeepParseIndentedCodeInterrupts
                && blockParserFactory instanceof IndentedCodeBlockParser.BlockFactory))
        && deepParser.isHtmlClosed();
  }

  @Override
  public boolean canContain(ParserState state, BlockParser blockParser, Block block) {
    return false;
  }

  @Override
  public boolean isInterruptible() {
    return myHtmlBlockDeepParseMarkdownInterruptsClosed
        && deepParser != null
        && deepParser.isHtmlClosed();
  }

  @Override
  public boolean isRawText() {
    return true;
  }

  @Override
  public void closeBlock(ParserState state) {
    block.setContent(content);
    content = null;

    // split out inner comments
    if (!(block instanceof HtmlCommentBlock) && parseInnerHtmlComments) {
      // need to break it up into non-comments and comments
      int lastIndex = 0;
      BasedSequence chars = block.getContentChars();
      if (chars.eolEndLength() > 0) {
        chars = chars.midSequence(0, -1);
      }

      int length = chars.length();
      while (lastIndex < length) {
        // find the opening HTML comment
        int index = chars.indexOf(HTML_COMMENT_OPEN, lastIndex);
        if (index < 0) {
          break;
        }

        // now lets find -->
        int end = chars.indexOf(HTML_COMMENT_CLOSE, index + HTML_COMMENT_OPEN.length());

        // if unterminated, ignore
        if (end < 0) {
          break;
        }

        if (lastIndex < index) {
          HtmlInnerBlock html = new HtmlInnerBlock(chars.subSequence(lastIndex, index));
          block.appendChild(html);
        }

        lastIndex = end + HTML_COMMENT_CLOSE.length();
        HtmlInnerBlockComment htmlComment =
            new HtmlInnerBlockComment(chars.subSequence(index, lastIndex));
        block.appendChild(htmlComment);
      }

      if (lastIndex > 0) {
        if (lastIndex < chars.length()) {
          HtmlInnerBlock html = new HtmlInnerBlock(chars.subSequence(lastIndex, chars.length()));
          block.appendChild(html);
        }
      }
    }
  }

  public static class Factory implements CustomBlockParserFactory {

    @Override
    public Set<Class<?>> getAfterDependents() {
      return new HashSet<>(
          Arrays.asList(
              BlockQuoteParser.Factory.class,
              HeadingParser.Factory.class,
              FencedCodeBlockParser.Factory.class
              // HtmlBlockParser.Factory.class,
              // ThematicBreakParser.Factory.class,
              // ListBlockParser.Factory.class,
              // IndentedCodeBlockParser.Factory.class
              ));
    }

    @Override
    public Set<Class<?>> getBeforeDependents() {
      return new HashSet<>(
          Arrays.asList(
              // BlockQuoteParser.Factory.class,
              // HeadingParser.Factory.class,
              // FencedCodeBlockParser.Factory.class,
              // HtmlBlockParser.Factory.class,
              ThematicBreakParser.Factory.class,
              ListBlockParser.Factory.class,
              IndentedCodeBlockParser.Factory.class));
    }

    @Override
    public boolean affectsGlobalScope() {
      return false;
    }

    @Override
    public BlockParserFactory apply(DataHolder options) {
      return new BlockFactory(options);
    }
  }

  private static class BlockFactory extends AbstractBlockParserFactory {
    private Patterns myPatterns = null;
    private final boolean myHtmlCommentBlocksInterruptParagraph;
    private final boolean myHtmlBlockDeepParser;
    private final boolean myHtmlBlockDeepParseNonBlock;
    private final boolean myHtmlBlockDeepParseFirstOpenTagOnOneLine;
    private final boolean myHtmlBlockCommentOnlyFullLine;
    private final boolean myHtmlBlockStartOnlyOnBlockTags;

    private BlockFactory(DataHolder options) {
      super();
      myHtmlCommentBlocksInterruptParagraph =
          Parser.HTML_COMMENT_BLOCKS_INTERRUPT_PARAGRAPH.get(options);
      this.myHtmlBlockDeepParser = Parser.HTML_BLOCK_DEEP_PARSER.get(options);
      this.myHtmlBlockDeepParseNonBlock = Parser.HTML_BLOCK_DEEP_PARSE_NON_BLOCK.get(options);
      this.myHtmlBlockDeepParseFirstOpenTagOnOneLine =
          Parser.HTML_BLOCK_DEEP_PARSE_FIRST_OPEN_TAG_ON_ONE_LINE.get(options);
      this.myHtmlBlockCommentOnlyFullLine = Parser.HTML_BLOCK_COMMENT_ONLY_FULL_LINE.get(options);
      this.myHtmlBlockStartOnlyOnBlockTags =
          Parser.HTML_BLOCK_START_ONLY_ON_BLOCK_TAGS.get(options);
    }

    @Override
    public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
      int nextNonSpace = state.getNextNonSpaceIndex();
      BasedSequence line = state.getLine();

      if (state.getIndent() < 4
          && line.charAt(nextNonSpace) == '<'
          && !(matchedBlockParser.getBlockParser() instanceof HtmlBlockParser)) {
        if (myHtmlBlockDeepParser) {
          HtmlDeepParser deepParser =
              new HtmlDeepParser(Parser.HTML_BLOCK_TAGS.get(state.getProperties()));
          deepParser.parseHtmlChunk(
              line.subSequence(nextNonSpace, line.length()),
              myHtmlBlockStartOnlyOnBlockTags,
              myHtmlBlockDeepParseNonBlock,
              myHtmlBlockDeepParseFirstOpenTagOnOneLine);
          if (deepParser.hadHtml()) {
            // have our html block start
            if ((deepParser.getHtmlMatch() == OPEN_TAG
                    || (!myHtmlCommentBlocksInterruptParagraph
                        && deepParser.getHtmlMatch() == COMMENT))
                && (!deepParser.isFirstBlockTag()
                    && matchedBlockParser.getBlockParser().getBlock() instanceof Paragraph)) {
            } else {
              // not paragraph or can interrupt paragraph
              return BlockStart.of(
                      new HtmlBlockParser(
                          state.getProperties(),
                          null,
                          deepParser.getHtmlMatch() == COMMENT,
                          deepParser))
                  .atIndex(state.getIndex());
            }
          }
        } else {
          for (int blockType = 1; blockType <= 7; blockType++) {
            // Type 7 cannot interrupt a paragraph or may not start a block altogether
            if (blockType == 7
                && (myHtmlBlockStartOnlyOnBlockTags
                    || matchedBlockParser.getBlockParser().getBlock() instanceof Paragraph)) {
              continue;
            }

            if (myPatterns == null) {
              myPatterns = new Patterns(state.getParsing(), state.getProperties());
            }

            Pattern opener = myPatterns.blockPatterns[blockType][0];
            Pattern closer = myPatterns.blockPatterns[blockType][1];
            Matcher matcher = opener.matcher(line.subSequence(nextNonSpace, line.length()));
            boolean matches = matcher.find();

            // TEST: non-interrupting of paragraphs by HTML comments
            if (matches
                && (myHtmlCommentBlocksInterruptParagraph
                    || blockType != myPatterns.commentPatternIndex
                    || !(matchedBlockParser.getBlockParser() instanceof ParagraphParser))) {
              // Issue #158, HTML Comment followed by text
              if (blockType == myPatterns.commentPatternIndex && myHtmlBlockCommentOnlyFullLine) {
                Matcher endMatcher =
                    myPatterns.blockPatterns[myPatterns.commentPatternIndex][1].matcher(
                        line.subSequence(matcher.end(), line.length()));
                if (endMatcher.find()) {
                  // see if nothing follows
                  BasedSequence trailing = line.subSequence(endMatcher.end(), line.length()).trim();
                  if (!trailing.equals("-->")) {
                    return BlockStart.none();
                  }
                }
              }
              return BlockStart.of(
                      new HtmlBlockParser(
                          state.getProperties(),
                          closer,
                          blockType == myPatterns.commentPatternIndex,
                          null))
                  .atIndex(state.getIndex());
            }
          }
        }
      }
      return BlockStart.none();
    }
  }
}
