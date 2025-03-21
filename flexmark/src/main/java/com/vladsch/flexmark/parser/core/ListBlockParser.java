package com.vladsch.flexmark.parser.core;

import static com.vladsch.flexmark.parser.Parser.BLANK_LINES_IN_AST;
import static com.vladsch.flexmark.util.data.SharedDataKeys.ESCAPE_NUMBERED_LEAD_IN;

import com.vladsch.flexmark.ast.BulletList;
import com.vladsch.flexmark.ast.ListBlock;
import com.vladsch.flexmark.ast.ListItem;
import com.vladsch.flexmark.ast.OrderedList;
import com.vladsch.flexmark.ast.util.Parsing;
import com.vladsch.flexmark.parser.ListOptions;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.parser.ParserEmulationProfile;
import com.vladsch.flexmark.parser.block.AbstractBlockParser;
import com.vladsch.flexmark.parser.block.AbstractBlockParserFactory;
import com.vladsch.flexmark.parser.block.BlockContinue;
import com.vladsch.flexmark.parser.block.BlockParser;
import com.vladsch.flexmark.parser.block.BlockParserFactory;
import com.vladsch.flexmark.parser.block.BlockStart;
import com.vladsch.flexmark.parser.block.CustomBlockParserFactory;
import com.vladsch.flexmark.parser.block.MatchedBlockParser;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.util.ast.BlankLine;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.collection.iteration.ReversiblePeekingIterator;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.misc.CharPredicate;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.SequenceUtils;
import com.vladsch.flexmark.util.sequence.mappers.SpecialLeadInCharsHandler;
import com.vladsch.flexmark.util.sequence.mappers.SpecialLeadInHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;

public class ListBlockParser extends AbstractBlockParser {
  private final ListBlock myBlock;
  private final ListOptions myOptions;
  private final ListData myListData;
  private ListItemParser myLastChild = null;
  private BasedSequence myItemHandledLine = null;
  private boolean myItemHandledNewListLine;
  private boolean myItemHandledNewItemLine;
  private boolean myItemHandledSkipActiveLine;

  private ListBlockParser(ListOptions options, ListData listData, ListItemParser listItemParser) {
    myOptions = options;
    myListData = listData;
    myBlock = listData.listBlock;
    myBlock.setTight(true);
    myLastChild = listItemParser;
    myItemHandledNewListLine = false;
    myItemHandledNewItemLine = false;
    myItemHandledSkipActiveLine = false;
  }

  void setItemHandledLine(BasedSequence itemHandledLine) {
    myItemHandledLine = itemHandledLine;
    myItemHandledNewListLine = false;
    myItemHandledNewItemLine = false;
    myItemHandledSkipActiveLine = false;
  }

  void setItemHandledNewListLine(BasedSequence itemHandledLine) {
    myItemHandledLine = itemHandledLine;
    myItemHandledNewListLine = true;
    myItemHandledNewItemLine = false;
    myItemHandledSkipActiveLine = false;
  }

  void setItemHandledNewItemLine(BasedSequence itemHandledLine) {
    myItemHandledLine = itemHandledLine;
    myItemHandledNewListLine = false;
    myItemHandledNewItemLine = true;
    myItemHandledSkipActiveLine = false;
  }

  void setItemHandledLineSkipActive(BasedSequence itemHandledLine) {
    myItemHandledLine = itemHandledLine;
    myItemHandledNewListLine = false;
    myItemHandledNewItemLine = false;
    myItemHandledSkipActiveLine = true;
  }

  public ListItemParser getLastChild() {
    return myLastChild;
  }

  public void setLastChild(ListItemParser lastChild) {
    myLastChild = lastChild;
  }

  public ListOptions getOptions() {
    return myOptions;
  }

  public ListData getListData() {
    return myListData;
  }

  @Override
  public boolean isContainer() {
    return true;
  }

  @Override
  public boolean canContain(ParserState state, BlockParser blockParser, Block block) {
    return block instanceof ListItem;
  }

  @Override
  public ListBlock getBlock() {
    return myBlock;
  }

  private void setTight(boolean tight) {
    myBlock.setTight(tight);
  }

  @Override
  public void closeBlock(ParserState state) {
    finalizeListTight(state);

    if (BLANK_LINES_IN_AST.get(state.getProperties())) {
      // need to transfer trailing blank line nodes from last item to parent list
      ListBlock block = getBlock();

      Node child = block.getFirstChildAnyNot(BlankLine.class);

      while (child instanceof ListItem) {
        // transfer its trailing blank lines to us
        child.moveTrailingBlankLines();
        child = child.getNextAnyNot(BlankLine.class);
      }
    }

    myBlock.setCharsFromContentOnly();
  }

  @Override
  public boolean breakOutOnDoubleBlankLine() {
    return myOptions.isEndOnDoubleBlank();
  }

  private static boolean hasNonItemChildren(ListItem item) {
    if (item.hasChildren()) {
      int count = 0;
      for (Node child : item.getChildren()) {
        if (child instanceof ListBlock) {
          continue;
        }

        count++;
        if (count >= 2) {
          return true;
        }
      }
    }
    return false;
  }

  private void finalizeListTight(ParserState parserState) {
    Node item = getBlock().getFirstChild();
    boolean isTight = true;
    boolean prevItemHadTrueTrailingBlankLine = false;
    boolean haveNestedList = false;

    while (item != null) {
      // check for non-final list item ending with blank line:
      boolean thisItemHadBlankAfterItemPara = false;
      boolean thisItemContainsBlankLine = false;
      boolean thisItemHadTrueTrailingBlankLine = false;
      boolean thisItemHadChildren = false;
      boolean thisItemLoose = false;

      if (item instanceof ListItem) {
        if (((ListItem) item).isHadBlankAfterItemParagraph()) {
          if (item.getNext() == null
              && (item.getFirstChild() == null || item.getFirstChild().getNext() == null)) {
            // not for last block
          } else {
            thisItemHadBlankAfterItemPara = true;
          }
        }

        if (((ListItem) item).isContainsBlankLine()) {
          thisItemContainsBlankLine = true;
        }

        if (parserState.endsWithBlankLine(item) && item.getNext() != null) {
          thisItemHadTrueTrailingBlankLine = true;
        }

        if (hasNonItemChildren((ListItem) item)) {
          thisItemHadChildren = true;
        }

        thisItemLoose =
            thisItemHadTrueTrailingBlankLine && myOptions.isLooseWhenHasTrailingBlankLine()
                || thisItemHadBlankAfterItemPara
                    && myOptions.isLooseWhenBlankLineFollowsItemParagraph()
                || thisItemContainsBlankLine && myOptions.isLooseWhenContainsBlankLine()
                || thisItemHadChildren && myOptions.isLooseWhenHasNonListChildren()
                || (thisItemHadTrueTrailingBlankLine && item.getPrevious() == null
                        || prevItemHadTrueTrailingBlankLine)
                    && (myOptions.isLooseWhenPrevHasTrailingBlankLine()
                        || (myOptions.isLooseWhenLastItemPrevHasTrailingBlankLine()
                            && item.getNext() == null));

        if (thisItemLoose) {
          ((ListItem) item).setLoose(true);
          isTight = false;
        }
      }

      // recurse into children of list item, to see if there are
      // spaces between any of them:
      Node subItem = item.getFirstChild();
      while (subItem != null) {
        if (parserState.endsWithBlankLine(subItem)
            && (item.getNext() != null || subItem.getNext() != null)) {
          if (subItem == item.getLastChild()) thisItemHadTrueTrailingBlankLine = true;

          if (!thisItemLoose) {
            if (myOptions.isLooseWhenHasTrailingBlankLine()) {
              isTight = false;
            }

            if (thisItemHadTrueTrailingBlankLine
                && item.getPrevious() == null
                && myOptions.isLooseWhenPrevHasTrailingBlankLine()) {
              isTight = false;
              thisItemLoose = true;
              ((ListItem) item).setLoose(true);
            }
          }
        }

        if (subItem instanceof ListBlock) {
          haveNestedList = true;
          if (!thisItemLoose && myOptions.isLooseWhenHasLooseSubItem()) {
            ReversiblePeekingIterator<Node> iterator = subItem.getChildIterator();
            while (iterator.hasNext()) {
              ListItem item1 = (ListItem) iterator.next();
              if (!item1.isTight()) {
                thisItemLoose = true;
                isTight = false;
                ((ListItem) item).setLoose(true);
                break;
              }
            }
          }
        }

        if (myOptions.isLooseWhenHasLooseSubItem()) {
          if (thisItemLoose && (haveNestedList || !myOptions.isAutoLooseOneLevelLists())) {
            break;
          }
        } else {
          if (!isTight && (haveNestedList || !myOptions.isAutoLooseOneLevelLists())) {
            break;
          }
        }
        subItem = subItem.getNext();
      }

      if (item instanceof ListItem) {
        prevItemHadTrueTrailingBlankLine = thisItemHadTrueTrailingBlankLine;
      }

      item = item.getNext();
    }

    if (myOptions.isAutoLoose() && myOptions.isAutoLooseOneLevelLists()) {
      if (!haveNestedList && getBlock().getAncestorOfType(ListBlock.class) == null && !isTight) {
        setTight(false);
      }
    } else {
      if (myOptions.isAutoLoose() && !isTight) {
        setTight(false);
      }
    }
  }

  /** Parse a list marker and return data on the marker or null. */
  static ListData parseListMarker(ListOptions options, int newItemCodeIndent, ParserState state) {
    Parsing parsing = state.getParsing();
    BasedSequence line = state.getLine();
    int markerIndex = state.getNextNonSpaceIndex();
    int markerColumn = state.getColumn() + state.getIndent();
    int markerIndent = state.getIndent();

    BasedSequence rest = line.subSequence(markerIndex, line.length());
    Matcher matcher = parsing.listItemMarker.matcher(rest);
    if (!matcher.find()) {
      return null;
    }

    ListBlock listBlock = createListBlock(matcher);

    int markerLength = matcher.end() - matcher.start();
    boolean isNumberedList = !"+-*".contains(matcher.group());
    int indexAfterMarker = markerIndex + markerLength;

    // marker doesn't include tabs, so counting them as columns directly is ok
    int columnAfterMarker = markerColumn + markerLength;

    // the column within the line where the content starts
    int contentOffset = 0;

    // See at which column the content starts if there is content
    boolean hasContent = false;
    int contentIndex = indexAfterMarker;
    for (int i = indexAfterMarker; i < line.length(); i++) {
      char c = line.charAt(i);
      if (c == '\t') {
        contentOffset += Parsing.columnsToNextTabStop(columnAfterMarker + contentOffset);
        contentIndex++;
      } else if (c == ' ') {
        contentOffset++;
        contentIndex++;
      } else {
        hasContent = true;
        break;
      }
    }

    BasedSequence markerSuffix = BasedSequence.NULL;
    int markerSuffixOffset = contentOffset;

    if (!hasContent || contentOffset > newItemCodeIndent) {
      // If this line is blank or has a code block, default to 1 space after marker
      markerSuffixOffset = contentOffset = 1;
    } else if (!isNumberedList || options.isNumberedItemMarkerSuffixed()) {
      // see if we have optional suffix strings on the marker
      String[] markerSuffixes = options.getItemMarkerSuffixes();
      for (String suffix : markerSuffixes) {
        int suffixLength = suffix.length();
        if (suffixLength > 0 && line.matchChars(suffix, contentIndex)) {
          if (options.isItemMarkerSpace()) {
            char c = line.midCharAt(contentIndex + suffixLength);
            if (c != ' ' && c != '\t') {
              // no space after, no match
              continue;
            }
          }

          markerSuffix = line.subSequence(contentIndex, contentIndex + suffixLength);
          contentOffset += suffixLength;
          contentIndex += suffixLength;
          columnAfterMarker += suffixLength;

          hasContent = false;
          int suffixContentOffset = contentOffset;

          for (int i = contentIndex; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\t') {
              contentOffset += Parsing.columnsToNextTabStop(columnAfterMarker + contentOffset);
            } else if (c == ' ') {
              contentOffset++;
            } else {
              hasContent = true;
              break;
            }
          }

          if (!hasContent || contentOffset - suffixContentOffset > newItemCodeIndent) {
            // If this line is blank or has a code block, default to 1 space after marker suffix
            contentOffset = suffixContentOffset + 1;
          }
          break;
        }
      }
    }

    return new ListData(
        listBlock,
        !hasContent,
        markerIndex,
        markerColumn,
        markerIndent,
        contentOffset,
        rest.subSequence(matcher.start(), matcher.end()),
        isNumberedList,
        markerSuffix,
        markerSuffixOffset);
  }

  private static ListBlock createListBlock(Matcher matcher) {
    String bullet = matcher.group(1);

    if (bullet != null) {
      BulletList bulletList = new BulletList();
      bulletList.setOpeningMarker(bullet.charAt(0));
      return bulletList;
    }

    String digit = matcher.group(2);
    String delimiter = matcher.group(3);
    OrderedList orderedList = new OrderedList();
    orderedList.setStartNumber(Integer.parseInt(digit));
    orderedList.setDelimiter(delimiter.charAt(0));
    return orderedList;
  }

  @Override
  public BlockContinue tryContinue(ParserState state) {
    // List blocks themselves don't have any markers, only list items. So try to stay in the list.
    // If there is a block start other than list item, canContain makes sure that this list is
    // closed.
    return BlockContinue.atIndex(state.getIndex());
  }

  public static class Factory implements CustomBlockParserFactory {

    @Override
    public Set<Class<?>> getAfterDependents() {
      return new HashSet<>(
          Arrays.asList(
              BlockQuoteParser.Factory.class,
              HeadingParser.Factory.class,
              FencedCodeBlockParser.Factory.class,
              HtmlBlockParser.Factory.class,
              ThematicBreakParser.Factory.class));
    }

    @Override
    public Set<Class<?>> getBeforeDependents() {
      Set<Class<?>> set = new HashSet<>();
      set.add(IndentedCodeBlockParser.Factory.class);
      return set;
    }

    @Override
    public boolean affectsGlobalScope() {
      return false;
    }

    @Override
    public BlockParserFactory apply(DataHolder options) {
      return new BlockFactory(options);
    }

    @Override
    public SpecialLeadInHandler getLeadInHandler(DataHolder options) {
      return ListItemLeadInHandler.create(
          Parser.LISTS_ITEM_PREFIX_CHARS.get(options),
          Parser.LISTS_ORDERED_ITEM_DOT_ONLY.get(options));
    }
  }

  private static class ListItemLeadInHandler extends SpecialLeadInCharsHandler {
    private static final CharPredicate ORDERED_DELIM_DOT = CharPredicate.anyOf('.');
    private static final CharPredicate ORDERED_DELIM_DOT_PARENS = CharPredicate.anyOf(".)");
    private static final SpecialLeadInHandler ORDERED_DELIM_DOT_HANDLER =
        new ListItemLeadInHandler(Parser.LISTS_ITEM_PREFIX_CHARS.getDefaultValue(), true);
    private static final SpecialLeadInHandler ORDERED_DELIM_DOT_PARENS_HANDLER =
        new ListItemLeadInHandler(Parser.LISTS_ITEM_PREFIX_CHARS.getDefaultValue(), false);

    static SpecialLeadInHandler create(CharSequence listItemDelims, boolean dotOnly) {
      return SequenceUtils.equals(Parser.LISTS_ITEM_PREFIX_CHARS.getDefaultValue(), listItemDelims)
          ? dotOnly ? ORDERED_DELIM_DOT_HANDLER : ORDERED_DELIM_DOT_PARENS_HANDLER
          : new ListItemLeadInHandler(listItemDelims, dotOnly);
    }

    private final CharPredicate orderedDelims;

    private ListItemLeadInHandler(CharSequence listItemDelims, boolean dotOnly) {
      super(CharPredicate.anyOf(listItemDelims));
      this.orderedDelims = dotOnly ? ORDERED_DELIM_DOT : ORDERED_DELIM_DOT_PARENS;
    }

    @Override
    public boolean escape(
        BasedSequence sequence, DataHolder options, Consumer<CharSequence> consumer) {
      if (super.escape(sequence, options, consumer)) {
        return true;
      }
      if (ESCAPE_NUMBERED_LEAD_IN.get(options)) {
        int nonDigit = sequence.indexOfAnyNot(CharPredicate.DECIMAL_DIGITS);
        if (nonDigit > 0
            && nonDigit + 1 == sequence.length()
            && orderedDelims.test(sequence.charAt(nonDigit))) {
          consumer.accept(sequence.subSequence(0, nonDigit));
          consumer.accept("\\");
          consumer.accept(sequence.subSequence(nonDigit));
          return true;
        }
      }
      return false;
    }

    @Override
    public boolean unEscape(
        BasedSequence sequence, DataHolder options, Consumer<CharSequence> consumer) {
      if (super.unEscape(sequence, options, consumer)) {
        return true;
      }
      int nonDigit = sequence.indexOfAnyNot(CharPredicate.DECIMAL_DIGITS);
      if (nonDigit > 0
          && nonDigit + 2 == sequence.length()
          && sequence.charAt(nonDigit) == '\\'
          && orderedDelims.test(sequence.charAt(nonDigit + 1))) {
        consumer.accept(sequence.subSequence(0, nonDigit));
        consumer.accept(sequence.subSequence(nonDigit + 1));
        return true;
      }
      return false;
    }
  }

  private static class BlockFactory extends AbstractBlockParserFactory {
    private final ListOptions myOptions;

    BlockFactory(DataHolder options) {
      super();
      myOptions = ListOptions.get(options);
    }

    @Override
    public BlockStart tryStart(ParserState state, MatchedBlockParser matchedBlockParser) {
      BlockParser matched = matchedBlockParser.getBlockParser();
      ParserEmulationProfile emulationFamily = myOptions.getParserEmulationProfile().family;
      int newItemCodeIndent = myOptions.getNewItemCodeIndent();

      if (matched instanceof ListBlockParser) {
        // the list item should have handled this line, if it is part of new list item, or a new sub
        // list
        ListBlockParser listBlockParser = (ListBlockParser) matched;

        if (state.getLine() == listBlockParser.myItemHandledLine) {
          if (listBlockParser.myItemHandledNewListLine) {
            // it is a new list already determined by the item
            ListData listData = parseListMarker(myOptions, newItemCodeIndent, state);
            ListItemParser listItemParser = new ListItemParser(myOptions, listData);

            int newColumn =
                listData.markerColumn + listData.listMarker.length() + listData.contentOffset;
            listBlockParser = new ListBlockParser(myOptions, listData, listItemParser);
            return BlockStart.of(listBlockParser, listItemParser).atColumn(newColumn);
          } else if (listBlockParser.myItemHandledNewItemLine) {
            // it is a new item for this list already determined by the previous item
            ListData listData = parseListMarker(myOptions, newItemCodeIndent, state);
            ListItemParser listItemParser = new ListItemParser(myOptions, listData);

            int newColumn =
                listData.markerColumn + listData.listMarker.length() + listData.contentOffset;
            listBlockParser.myLastChild = listItemParser;

            return BlockStart.of(listItemParser).atColumn(newColumn);
          }

          // then it is not for us to handle, since we only handle new list creation here outside of
          // an existing list
          listBlockParser.myItemHandledLine = null;
          return BlockStart.none();
        }

        // if there is a pre-existing list then it's last list item should have handled the line
        return BlockStart.none();
      }

      // see if the list item is still active and set line handled, need this to handle lazy
      // continuations when they look like list items
      ListBlock block = (ListBlock) matched.getBlock().getAncestorOfType(ListBlock.class);
      if (block != null) {
        ListBlockParser listBlockParser = (ListBlockParser) state.getActiveBlockParser(block);
        if (listBlockParser.myItemHandledLine == state.getLine()
            && listBlockParser.myItemHandledSkipActiveLine) {
          listBlockParser.myItemHandledLine = null;
          return BlockStart.none();
        }
      }

      // at this point if the line should have been handled by the the item
      // what we can have here is list items for the same list or list items that start a new list
      // because of mismatched type
      // nothing else should get here because the list item should have handled it
      // so everything should have indent >= current list indent, the rest should not be here

      if (emulationFamily == ParserEmulationProfile.COMMONMARK) {
        int currentIndent = state.getIndent();
        if (currentIndent >= myOptions.getCodeIndent()) {
          return BlockStart.none();
        }
      } else if (emulationFamily == ParserEmulationProfile.FIXED_INDENT) {
        int currentIndent = state.getIndent();
        if (currentIndent >= myOptions.getItemIndent()) {
          return BlockStart.none();
        }
      } else if (emulationFamily == ParserEmulationProfile.KRAMDOWN) {
        int currentIndent = state.getIndent();
        if (currentIndent >= myOptions.getItemIndent()) {
          return BlockStart.none();
        }
      } else if (emulationFamily == ParserEmulationProfile.MARKDOWN) {
        int currentIndent = state.getIndent();
        if (currentIndent >= myOptions.getItemIndent()) {
          return BlockStart.none();
        }
      }

      ListData listData = parseListMarker(myOptions, newItemCodeIndent, state);

      if (listData != null) {
        int newColumn =
            listData.markerColumn + listData.listMarker.length() + listData.contentOffset;

        boolean inParagraph = matched.isParagraphParser();
        boolean inParagraphListItem =
            inParagraph
                && matched.getBlock().getParent() instanceof ListItem
                && matched.getBlock() == matched.getBlock().getParent().getFirstChild();

        if (inParagraph
            && !myOptions.canInterrupt(listData.listBlock, listData.isEmpty, inParagraphListItem)) {
          return BlockStart.none();
        }

        ListItemParser listItemParser = new ListItemParser(myOptions, listData);

        // prepend new list block
        ListBlockParser listBlockParser = new ListBlockParser(myOptions, listData, listItemParser);
        return BlockStart.of(listBlockParser, listItemParser).atColumn(newColumn);
      }

      return BlockStart.none();
    }
  }

  static class ListData {
    final ListBlock listBlock;
    final boolean isEmpty;
    private final int markerColumn;
    final int markerIndent;
    final int contentOffset;
    final BasedSequence listMarker;
    final boolean isNumberedList;
    final BasedSequence markerSuffix;
    final int markerSuffixOffset;

    private ListData(
        ListBlock listBlock,
        boolean isEmpty,
        int markerIndex,
        int markerColumn,
        int markerIndent,
        int contentOffset,
        BasedSequence listMarker,
        boolean isNumberedList,
        BasedSequence markerSuffix,
        int markerSuffixOffset) {
      this.listBlock = listBlock;
      this.isEmpty = isEmpty;
      this.markerColumn = markerColumn;
      this.markerIndent = markerIndent;
      this.contentOffset = contentOffset;
      this.listMarker = listMarker;
      this.isNumberedList = isNumberedList;
      this.markerSuffix = markerSuffix;
      this.markerSuffixOffset = markerSuffixOffset;
    }
  }
}
