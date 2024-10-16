package com.vladsch.flexmark.util.format;

import com.vladsch.flexmark.util.collection.BoundedMaxAggregator;
import com.vladsch.flexmark.util.collection.BoundedMinAggregator;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

public class TableCellOffsetInfo {
  // Stop points used by next/prev tab navigation
  private static final int ROW_START = 0x0001;
  private static final int TEXT_START = 0x0002;
  private static final int TEXT_END = 0x0004;
  private static final int ROW_END = 0x0008;

  private static final Map<TableSectionType, Integer> DEFAULT_STOP_POINTS_MAP =
      new EnumMap<>(TableSectionType.class);

  static {
    DEFAULT_STOP_POINTS_MAP.put(TableSectionType.HEADER, TEXT_END);
    DEFAULT_STOP_POINTS_MAP.put(TableSectionType.SEPARATOR, TEXT_START | TEXT_END);
    DEFAULT_STOP_POINTS_MAP.put(TableSectionType.BODY, TEXT_END);
    DEFAULT_STOP_POINTS_MAP.put(TableSectionType.CAPTION, TEXT_END);
  }

  public final MarkdownTable table;
  public final int offset;
  public final TableSection section;
  final TableRow tableRow; // at or inside cell
  final TableCell tableCell; // at or inside cell
  private final int row; // all rows with separator index
  final int column; // at column right before or right after
  private final Integer insideColumn; // inside column or null
  private final Integer insideOffset; // offset from start of column or null if not inside column

  public TableCellOffsetInfo(
      int offset,
      MarkdownTable table,
      TableSection section,
      TableRow tableRow,
      TableCell tableCell,
      int row,
      int column,
      Integer insideColumn,
      Integer insideOffset) {
    this.offset = offset;
    this.table = table;
    this.section = section;
    this.tableRow = tableRow;
    this.tableCell = tableCell;
    this.row = row;
    this.column = column;
    this.insideColumn = insideColumn;
    this.insideOffset = insideOffset;
  }

  public boolean isCaptionLine() {
    return tableRow instanceof TableCaptionRow && section == table.caption;
  }

  public boolean isSeparatorLine() {
    return section.sectionType == TableSectionType.SEPARATOR;
  }

  public boolean isInsideCaption() {
    return isCaptionLine() && getInsideColumn();
  }

  public boolean isAfterCaption() {
    return isCaptionLine() && isAfterCells();
  }

  public boolean isBeforeCaption() {
    return isCaptionLine() && isBeforeCells();
  }

  public boolean isInsideCell() {
    return tableRow != null && tableCell != null && insideColumn != null;
  }

  public boolean getInsideColumn() {
    return insideColumn != null;
  }

  public boolean isBeforeCells() {
    return tableRow != null
        && tableCell != null
        && insideColumn == null
        && column < tableRow.cells.size()
        && offset <= tableCell.getStartOffset(getPreviousCell());
  }

  public TableCell getPreviousCell() {
    return getPreviousCell(1);
  }

  private TableCell getPreviousCell(int offset) {
    return getPreviousCell(tableRow, offset);
  }

  private TableCell getPreviousCell(TableRow tableRow, int offset) {
    return column >= offset && tableRow != null ? tableRow.cells.get(column - offset) : null;
  }

  public boolean isInCellSpan() {
    return tableRow != null
        && tableCell != null
        && insideColumn == null
        && offset >= tableCell.getStartOffset(getPreviousCell())
        && offset < tableCell.getEndOffset();
  }

  public boolean isAfterCells() {
    return tableRow != null
        && tableCell != null
        && insideColumn == null
        && column == tableRow.cells.size()
        && offset >= tableCell.getEndOffset();
  }

  public boolean isFirstCell() {
    return getInsideColumn() && column == 0;
  }

  public boolean isLastCell() {
    return getInsideColumn() && column + 1 == tableRow.cells.size();
  }

  public boolean isLastRow() {
    return row + 1 == table.getAllRowsCount();
  }

  /**
   * Available if somewhere in table
   *
   * @param stopPointsMap stop points of interest map by section or null
   * @return next stop point offset or offset after end of table
   */
  public TableCellOffsetInfo nextOffsetStop(Map<TableSectionType, Integer> stopPointsMap) {
    int stopOffset = getStopOffset(offset, table, stopPointsMap, true);
    if (stopOffset != -1) {
      return table.getCellOffsetInfo(stopOffset);
    }

    // go to after the table
    List<TableRow> allRows = table.getAllSectionRows();
    TableRow lastRow = allRows.get(allRows.size() - 1);
    TableCell lastCell = lastRow.cells.get(lastRow.cells.size() - 1);
    int offset = lastCell.getEndOffset();
    BasedSequence baseSequence = lastCell.text.getBaseSequence();

    int eolPos = baseSequence.endOfLineAnyEOL(offset);
    return table.getCellOffsetInfo(
        eolPos == -1 ? offset : eolPos + baseSequence.eolStartLength(eolPos));
  }

  /**
   * Available if somewhere in table
   *
   * @param stopPointsMap stop points of interest map by section or null for default
   * @return previous stop point offset or start of table offset
   */
  public TableCellOffsetInfo previousOffsetStop(Map<TableSectionType, Integer> stopPointsMap) {
    int stopOffset = getStopOffset(offset, table, stopPointsMap, false);
    if (stopOffset != -1) {
      return table.getCellOffsetInfo(stopOffset);
    }
    return table.getCellOffsetInfo(table.getTableStartOffset());
  }

  private static boolean haveStopPoint(int flags, int mask) {
    return (flags & mask) != 0;
  }

  private static boolean haveRowStart(int flags) {
    return (flags & ROW_START) != 0;
  }

  private static boolean haveRowEnd(int flags) {
    return (flags & ROW_END) != 0;
  }

  private static boolean haveTextStart(int flags) {
    return (flags & TEXT_START) != 0;
  }

  private static boolean haveTextEnd(int flags) {
    return (flags & TEXT_END) != 0;
  }

  /**
   * Return the next/previous stop point of interest
   *
   * <p>NOTE: not terribly efficient because it goes through all cells of all rows. Only intended
   * for UI use where this is not an issue since it is done per user key
   *
   * @param offset current offset
   * @param table for table
   * @param stopPointsMap map of stop points by section or null for default
   * @param nextOffset true if next offset stop point, false for previous stop point of interest
   * @return stop point found or -1 if not found
   */
  private static int getStopOffset(
      int offset,
      MarkdownTable table,
      Map<TableSectionType, Integer> stopPointsMap,
      boolean nextOffset) {
    Integer[] result = new Integer[] {null};

    Map<TableSectionType, Integer> useStopPointsMap =
        stopPointsMap == null ? DEFAULT_STOP_POINTS_MAP : stopPointsMap;
    BinaryOperator<Integer> aggregator =
        nextOffset ? new BoundedMinAggregator(offset) : new BoundedMaxAggregator(offset);

    table.forAllSectionRows(
        (row, allRowsIndex, sectionRows, sectionRowIndex) -> {
          TableSection section = table.getAllRowsSection(allRowsIndex);
          if (!row.cells.isEmpty() && useStopPointsMap.containsKey(section.sectionType)) {
            int flags = useStopPointsMap.get(section.sectionType);

            if (flags != 0) {
              int rowStart = row.cells.get(0).getStartOffset(null);
              int rowEnd = row.cells.get(row.cells.size() - 1).getEndOffset();

              if (haveRowStart(flags)) result[0] = aggregator.apply(result[0], rowStart);

              if (haveStopPoint(flags, TEXT_START | TEXT_END)) {
                TableCell previousCell = null;
                for (TableCell cell : row.cells) {
                  if (haveTextStart(flags)) {
                    int textStart = cell.getTextStartOffset(previousCell);
                    result[0] = aggregator.apply(result[0], textStart);
                  }

                  if (haveTextEnd(flags)) {
                    int textEnd = cell.getTextEndOffset(previousCell);
                    result[0] = aggregator.apply(result[0], textEnd);
                  }
                  previousCell = cell;
                }
              }

              if (haveRowEnd(flags)) result[0] = aggregator.apply(result[0], rowEnd);
            }
          }
          return 0;
        });

    return result[0] == null ? -1 : result[0];
  }

  @Override
  public String toString() {
    return "CellOffsetInfo{"
        + " offset="
        + offset
        + ", row="
        + row
        + ", column="
        + column
        + ", insideColumn="
        + insideColumn
        + ", insideOffset="
        + insideOffset
        + '}';
  }
}
