package com.vladsch.flexmark.ext.tables.internal;

import com.vladsch.flexmark.ast.Paragraph;
import com.vladsch.flexmark.ast.Text;
import com.vladsch.flexmark.ast.WhiteSpace;
import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCaption;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.ext.tables.TableSeparator;
import com.vladsch.flexmark.parser.InlineParser;
import com.vladsch.flexmark.parser.block.CharacterNodeFactory;
import com.vladsch.flexmark.parser.block.ParagraphPreProcessor;
import com.vladsch.flexmark.parser.block.ParagraphPreProcessorFactory;
import com.vladsch.flexmark.parser.block.ParserState;
import com.vladsch.flexmark.parser.core.ReferencePreProcessorFactory;
import com.vladsch.flexmark.util.ast.Block;
import com.vladsch.flexmark.util.ast.DoNotDecorate;
import com.vladsch.flexmark.util.ast.Node;
import com.vladsch.flexmark.util.ast.NodeIterator;
import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.format.TableFormatOptions;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class TableParagraphPreProcessor implements ParagraphPreProcessor {
  private static BitSet pipeCharacters = new BitSet();
  private static BitSet separatorCharacters = new BitSet();

  static {
    pipeCharacters.set('|');

    separatorCharacters.set('|');
    separatorCharacters.set(':');
    separatorCharacters.set('-');
  }

  private static final Map<Character, CharacterNodeFactory> pipeNodeMap = new HashMap<>();

  static {
    pipeNodeMap.put(
        '|',
        new CharacterNodeFactory() {
          @Override
          public boolean skipNext(char c) {
            return c == ' ' || c == '\t';
          }

          @Override
          public boolean skipPrev(char c) {
            return c == ' ' || c == '\t';
          }

          @Override
          public boolean wantSkippedWhitespace() {
            return true;
          }

          @Override
          public Node get() {
            return new TableColumnSeparator();
          }
        });
  }

  private static final Map<Character, CharacterNodeFactory> pipeIntelliJNodeMap = new HashMap<>();

  static {
    pipeIntelliJNodeMap.put(
        '|',
        new CharacterNodeFactory() {
          @Override
          public boolean skipNext(char c) {
            return c == ' ' || c == '\t';
          }

          @Override
          public boolean skipPrev(char c) {
            return c == ' ' || c == '\t';
          }

          @Override
          public boolean wantSkippedWhitespace() {
            return true;
          }

          @Override
          public Node get() {
            return new TableColumnSeparator();
          }
        });
  }

  public static ParagraphPreProcessorFactory factory() {
    return new ParagraphPreProcessorFactory() {
      @Override
      public boolean affectsGlobalScope() {
        return false;
      }

      @Override
      public Set<Class<?>> getAfterDependents() {
        Set<Class<?>> set = new HashSet<>();
        set.add(ReferencePreProcessorFactory.class);
        return set;
      }

      @Override
      public Set<Class<?>> getBeforeDependents() {
        return null;
      }

      @Override
      public ParagraphPreProcessor apply(ParserState state) {
        return new TableParagraphPreProcessor(state.getProperties());
      }
    };
  }

  private final TableParserOptions options;
  private final Pattern tableHeaderSeparator;

  private static Pattern getTableHeaderSeparator(
      int minColumnDashes, String intellijDummyIdentifier) {
    int minCol = minColumnDashes >= 1 ? minColumnDashes : 1;
    int minColDash = minColumnDashes >= 2 ? minColumnDashes - 1 : 1;
    int minColDashes = minColumnDashes >= 3 ? minColumnDashes - 2 : 1;
    // to prevent conversion to arabic numbers, using string
    String col =
        String.format(
            Locale.US,
            "(?:" + "\\s*-{%d,}\\s*|\\s*:-{%d,}\\s*|\\s*-{%d,}:\\s*|\\s*:-{%d,}:\\s*" + ")",
            minCol,
            minColDash,
            minColDash,
            minColDashes);

    boolean noIntelliJ = intellijDummyIdentifier.isEmpty();
    String add = noIntelliJ ? "" : TableFormatOptions.INTELLIJ_DUMMY_IDENTIFIER;
    String sp = noIntelliJ ? "\\s" : "(?:\\s" + add + "?)";
    String ds = noIntelliJ ? "-" : "(?:-" + add + "?)";
    String pipe = noIntelliJ ? "\\|" : "(?:" + add + "?\\|" + add + "?)";

    String regex =
        "\\|"
            + col
            + "\\|?\\s*"
            + "|"
            + col
            + "\\|\\s*"
            + "|"
            + "\\|?"
            + "(?:"
            + col
            + "\\|)+"
            + col
            + "\\|?\\s*";

    String withIntelliJ = regex.replace("\\s", sp).replace("\\|", pipe).replace("-", ds);

    return Pattern.compile(withIntelliJ);
  }

  private TableParagraphPreProcessor(DataHolder options) {
    this.options = new TableParserOptions(options);
    this.tableHeaderSeparator = getTableHeaderSeparator(this.options.minSeparatorDashes, "");
  }

  private static class TableSeparatorRow extends TableRow implements DoNotDecorate {
    TableSeparatorRow(BasedSequence chars) {
      super(chars);
    }
  }

  @Override
  public int preProcessBlock(Paragraph block, ParserState state) {
    InlineParser inlineParser = state.getInlineParser();

    List<BasedSequence> tableLines = new ArrayList<>();
    int separatorLineNumber = -1;
    BasedSequence separatorLine = null;
    int blockIndent = block.getLineIndent(0);
    BasedSequence captionLine = null;
    BitSet separators = separatorCharacters;
    Map<Character, CharacterNodeFactory> nodeMap = pipeNodeMap;

    for (BasedSequence rowLine : block.getContentLines()) {
      int rowNumber = tableLines.size();
      if (separatorLineNumber == -1 && rowNumber > options.maxHeaderRows)
        return 0; // too many header rows

      if (rowLine.indexOf('|') < 0) {
        if (separatorLineNumber == -1) {
          return 0;
        }

        if (options.withCaption) {
          BasedSequence trimmed = rowLine.trim();
          if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            captionLine = trimmed;
          }
        }
        break;
      }

      // NOTE: block lines now contain leading indent spaces which should be ignored
      BasedSequence trimmedRowLine = rowLine.subSequence(block.getLineIndent(rowNumber));

      if (separatorLineNumber == -1) {
        if (rowNumber >= options.minHeaderRows
            && tableHeaderSeparator.matcher(trimmedRowLine).matches()) {
          // must start with | or cell, whitespace means its not a separator line
          if (rowLine.charAt(0) != ' ' && rowLine.charAt(0) != '\t' || rowLine.charAt(0) != '|') {
            separatorLineNumber = rowNumber;
            separatorLine = trimmedRowLine;
          } else if (rowLine.charAt(0) == ' ' || rowLine.charAt(0) == '\t') {
            block.setHasTableSeparator(true);
          }
        }
      }

      tableLines.add(trimmedRowLine);
    }

    if (separatorLineNumber == -1) {
      return 0;
    }

    List<TableRow> tableRows = new ArrayList<>();
    for (BasedSequence rowLine : tableLines) {
      int rowNumber = tableRows.size();

      BasedSequence fullRowLine =
          block.getLineIndent(rowNumber) <= blockIndent
              ? rowLine.trimEOL()
              : rowLine.baseSubSequence(
                  rowLine.getStartOffset() - (block.getLineIndent(rowNumber) - blockIndent),
                  rowLine.getEndOffset() - rowLine.eolEndLength());
      boolean isSeparator = rowNumber == separatorLineNumber;
      TableRow tableRow = new TableRow(fullRowLine);
      int tableRowNumber;

      List<Node> sepList;
      if (isSeparator) {
        TableSeparatorRow fakeRow = new TableSeparatorRow(fullRowLine);
        sepList = inlineParser.parseCustom(fullRowLine, fakeRow, separators, nodeMap);
        tableRow.takeChildren(fakeRow);
        // sepList = inlineParser.parseCustom(fullRowLine, tableRow, separators, nodeMap);
        tableRowNumber = 0;
      } else {
        sepList = inlineParser.parseCustom(fullRowLine, tableRow, pipeCharacters, pipeNodeMap);
        if (rowNumber < separatorLineNumber) {
          tableRowNumber = rowNumber + 1;
        } else {
          tableRowNumber = rowNumber - separatorLineNumber;
        }

        // can have table separators embedded inside inline elements, need to convert them to text
        // and remove them from sepList
        if (sepList != null) {
          sepList = cleanUpInlinedSeparators(inlineParser, tableRow, sepList);
        }
      }

      if (sepList == null) {
        if (rowNumber <= separatorLineNumber) {
          return 0;
        }
        break;
      }

      tableRow.setRowNumber(tableRowNumber);
      tableRows.add(tableRow);
    }

    // table is done, could be earlier than the lines tested earlier, may need to truncate lines
    Block tableBlock = new TableBlock(tableLines.subList(0, tableRows.size()));
    Node section = new TableHead(tableLines.get(0).subSequence(0, 0));
    tableBlock.appendChild(section);

    List<TableCell.Alignment> alignments = parseAlignment(separatorLine);

    int rowNumber = 0;
    int separatorColumns = alignments.size();
    for (TableRow tableRow : tableRows) {
      if (rowNumber == separatorLineNumber) {
        section.setCharsFromContent();
        section = new TableSeparator();
        tableBlock.appendChild(section);
      } else if (rowNumber == separatorLineNumber + 1) {
        section.setCharsFromContent();
        section = new TableBody();
        tableBlock.appendChild(section);
      }

      boolean firstCell = true;
      int cellCount = 0;
      NodeIterator nodes = new NodeIterator(tableRow.getFirstChild());
      TableRow newTableRow = new TableRow(tableRow.getChars());
      newTableRow.setRowNumber(tableRow.getRowNumber());
      int accumulatedSpanOffset = 0;

      while (nodes.hasNext()) {
        if (cellCount >= separatorColumns && options.discardExtraColumns) {
          if (options.headerSeparatorColumnMatch && rowNumber < separatorLineNumber) {
            // header/separator mismatch
            return 0;
          }

          break;
        }

        TableCell tableCell = new TableCell();

        if (firstCell && nodes.peek() instanceof TableColumnSeparator) {
          Node columnSep = nodes.next();
          tableCell.setOpeningMarker(columnSep.getChars());
          columnSep.unlink();
          firstCell = false;
        }

        TableCell.Alignment alignment =
            cellCount + accumulatedSpanOffset < separatorColumns
                ? alignments.get(cellCount + accumulatedSpanOffset)
                : null;
        tableCell.setHeader(rowNumber < separatorLineNumber);
        tableCell.setAlignment(alignment);

        // take all until separator or end of iterator
        while (nodes.hasNext()) {
          if (nodes.peek() instanceof TableColumnSeparator) {
            break;
          }
          tableCell.appendChild(nodes.next());
        }

        // accumulate closers, and optional spans
        BasedSequence closingMarker = null;
        int span = 1;
        while (nodes.hasNext()) {
          if (!(nodes.peek() instanceof TableColumnSeparator)) {
            break;
          }
          if (closingMarker == null) {
            closingMarker = nodes.next().getChars();
            if (!options.columnSpans) {
              break;
            }
          } else {
            BasedSequence nextSep = nodes.peek().getChars();

            if (!closingMarker.isContinuedBy(nextSep)) {
              break;
            }
            closingMarker = closingMarker.spliceAtEnd(nextSep);
            nodes.next().unlink();
            span++;
          }
        }

        accumulatedSpanOffset += span - 1;

        if (closingMarker != null) {
          tableCell.setClosingMarker(closingMarker);
        }
        tableCell.setChars(tableCell.getChildChars());
        // option to keep cell whitespace, if yes, then convert it to text and merge adjacent text
        // nodes
        if (options.trimCellWhitespace) {
          tableCell.trimWhiteSpace();
        } else {
          tableCell.mergeWhiteSpace();
        }

        // NOTE: here we get only chars which do not reflect out-of-base characters, prefixes and
        // removed text
        tableCell.setText(tableCell.getChildChars());

        tableCell.setCharsFromContent();
        tableCell.setSpan(span);
        newTableRow.appendChild(tableCell);
        cellCount++;
      }

      if (options.headerSeparatorColumnMatch
          && rowNumber < separatorLineNumber
          && cellCount < separatorColumns) {
        // no match
        return 0;
      }

      while (options.appendMissingColumns && cellCount < separatorColumns) {
        TableCell tableCell = new TableCell();
        tableCell.setHeader(rowNumber < separatorLineNumber);
        tableCell.setAlignment(alignments.get(cellCount));
        newTableRow.appendChild(tableCell);
        cellCount++;
      }

      newTableRow.setCharsFromContent();
      section.appendChild(newTableRow);

      rowNumber++;
    }

    section.setCharsFromContent();

    if (section instanceof TableSeparator) {
      TableBody tableBody =
          new TableBody(section.getChars().subSequence(section.getChars().length()));
      tableBlock.appendChild(tableBody);
    }

    // Add caption if the option is enabled
    if (captionLine != null) {
      TableCaption caption =
          new TableCaption(
              captionLine.subSequence(0, 1),
              captionLine.subSequence(1, captionLine.length() - 1),
              captionLine.subSequence(captionLine.length() - 1));
      inlineParser.parse(caption.getText(), caption);
      caption.setCharsFromContent();
      tableBlock.appendChild(caption);
    }

    tableBlock.setCharsFromContent();

    block.insertBefore(tableBlock);
    state.blockAdded(tableBlock);
    return tableBlock.getChars().length();
  }

  private List<Node> cleanUpInlinedSeparators(
      InlineParser inlineParser, TableRow tableRow, List<Node> sepList) {
    // any separators which do not have tableRow as parent are embedded into inline elements and
    // should be
    // converted back to text
    List<Node> removedSeparators = null;
    List<Node> mergeTextParents = null;

    for (Node node : sepList) {
      if (node.getParent() != null && node.getParent() != tableRow) {
        // embedded, convert it and surrounding whitespace to text
        Node firstNode = node.getPrevious() instanceof WhiteSpace ? node.getPrevious() : node;
        Node lastNode = node.getNext() instanceof WhiteSpace ? node.getNext() : node;

        Text text =
            new Text(node.baseSubSequence(firstNode.getStartOffset(), lastNode.getEndOffset()));
        node.insertBefore(text);
        node.unlink();
        firstNode.unlink();
        lastNode.unlink();

        if (removedSeparators == null) {
          removedSeparators = new ArrayList<>();
          mergeTextParents = new ArrayList<>();
        }

        removedSeparators.add(node);
        mergeTextParents.add(text.getParent());
      }
    }

    if (mergeTextParents != null) {
      for (Node parent : mergeTextParents) {
        inlineParser.mergeTextNodes(parent.getFirstChild(), parent.getLastChild());
      }

      if (removedSeparators.size() == sepList.size()) {
        return null;
      }

      List<Node> newSeparators = new ArrayList<>(sepList);
      newSeparators.removeAll(removedSeparators);
      return newSeparators;
    }

    return sepList;
  }

  private static List<TableCell.Alignment> parseAlignment(BasedSequence separatorLine) {
    List<BasedSequence> parts = split(separatorLine, false, false);
    List<TableCell.Alignment> alignments = new ArrayList<>();
    for (BasedSequence part : parts) {
      BasedSequence trimmed = part.trim();
      boolean left = trimmed.startsWith(":");
      boolean right = trimmed.endsWith(":");
      TableCell.Alignment alignment = getAlignment(left, right);
      alignments.add(alignment);
    }
    return alignments;
  }

  private static List<BasedSequence> split(
      BasedSequence input, boolean columnSpans, boolean wantPipes) {
    BasedSequence line = input.trim();
    int lineLength = line.length();
    List<BasedSequence> segments = new ArrayList<>();

    if (line.startsWith("|")) {
      if (wantPipes) segments.add(line.subSequence(0, 1));
      line = line.subSequence(1, lineLength);
      lineLength--;
    }

    boolean escape = false;
    int lastPos = 0;
    int cellChars = 0;
    for (int i = 0; i < lineLength; i++) {
      char c = line.charAt(i);
      if (escape) {
        escape = false;
        cellChars++;
      } else {
        switch (c) {
          case '\\':
            escape = true;
            // Removing the escaping '\' is handled by the inline parser later, so add it to cell
            cellChars++;
            break;
          case '|':
            if (!columnSpans || lastPos < i) segments.add(line.subSequence(lastPos, i));
            if (wantPipes) segments.add(line.subSequence(i, i + 1));
            lastPos = i + 1;
            cellChars = 0;
            break;
          default:
            cellChars++;
        }
      }
    }

    if (cellChars > 0) {
      segments.add(line.subSequence(lastPos, lineLength));
    }
    return segments;
  }

  private static TableCell.Alignment getAlignment(boolean left, boolean right) {
    if (left && right) {
      return TableCell.Alignment.CENTER;
    } else if (left) {
      return TableCell.Alignment.LEFT;
    } else if (right) {
      return TableCell.Alignment.RIGHT;
    } else {
      return null;
    }
  }
}
