package com.vladsch.flexmark.ext.gfm.tasklist;

import com.vladsch.flexmark.ext.gfm.tasklist.internal.TaskListItemBlockPreProcessor;
import com.vladsch.flexmark.ext.gfm.tasklist.internal.TaskListNodeFormatter;
import com.vladsch.flexmark.ext.gfm.tasklist.internal.TaskListNodeRenderer;
import com.vladsch.flexmark.formatter.Formatter;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.ListOptions;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.MutableDataHolder;
import java.util.HashMap;
import java.util.Map;

/**
 * Extension for GFM style task list items
 *
 * <p>The bullet list items that begin with [ ], [x] or [X] are turned into TaskListItem nodes
 */
public class TaskListExtension
    implements Parser.ParserExtension,
        HtmlRenderer.HtmlRendererExtension,
        Formatter.FormatterExtension {
  private static final Map<Character, Integer> DEFAULT_PRIORITIES = new HashMap<>();

  static {
    DEFAULT_PRIORITIES.put('+', 1);
    DEFAULT_PRIORITIES.put('*', 0);
    DEFAULT_PRIORITIES.put('-', -1);
  }

  public static final DataKey<String> ITEM_DONE_MARKER =
      new DataKey<>(
          "ITEM_DONE_MARKER",
          "<input type=\"checkbox\" class=\"task-list-item-checkbox\" checked=\"checked\" disabled=\"disabled\" readonly=\"readonly\" />&nbsp;");
  public static final DataKey<String> ITEM_NOT_DONE_MARKER =
      new DataKey<>(
          "ITEM_NOT_DONE_MARKER",
          "<input type=\"checkbox\" class=\"task-list-item-checkbox\" disabled=\"disabled\" readonly=\"readonly\" />&nbsp;");
  public static final DataKey<String> TIGHT_ITEM_CLASS =
      new DataKey<>("TIGHT_ITEM_CLASS", "task-list-item");

  public static final DataKey<String> LOOSE_ITEM_CLASS =
      new DataKey<>("LOOSE_ITEM_CLASS", TIGHT_ITEM_CLASS);
  public static final DataKey<String> PARAGRAPH_CLASS = new DataKey<>("PARAGRAPH_CLASS", "");
  public static final DataKey<String> ITEM_DONE_CLASS = new DataKey<>("ITEM_DONE_CLASS", "");
  public static final DataKey<String> ITEM_NOT_DONE_CLASS =
      new DataKey<>("ITEM_NOT_DONE_CLASS", "");

  // formatting options
  public static final DataKey<TaskListItemCase> FORMAT_LIST_ITEM_CASE =
      new DataKey<>("FORMAT_LIST_ITEM_CASE", TaskListItemCase.AS_IS);
  public static final DataKey<TaskListItemPlacement> FORMAT_LIST_ITEM_PLACEMENT =
      new DataKey<>("FORMAT_LIST_ITEM_PLACEMENT", TaskListItemPlacement.AS_IS);
  public static final DataKey<Integer> FORMAT_ORDERED_TASK_ITEM_PRIORITY =
      new DataKey<>("FORMAT_ORDERED_TASK_ITEM_PRIORITY", 0);
  public static final DataKey<Integer> FORMAT_DEFAULT_TASK_ITEM_PRIORITY =
      new DataKey<>("FORMAT_DEFAULT_TASK_ITEM_PRIORITY", 0);
  public static final DataKey<Boolean> FORMAT_PRIORITIZED_TASK_ITEMS =
      new DataKey<>("FORMAT_PRIORITIZED_TASK_ITEMS", false);

  /**
   * Priorities corresponding to {@link Parser#LISTS_ITEM_PREFIX_CHARS} If shorter than item prefix
   * chars then any missing priorities are set to 0
   */
  public static final DataKey<Map<Character, Integer>> FORMAT_TASK_ITEM_PRIORITIES =
      new DataKey<>("FORMAT_TASK_ITEM_PRIORITIES", DEFAULT_PRIORITIES);

  TaskListExtension() {}

  public static TaskListExtension create() {
    return new TaskListExtension();
  }

  @Override
  public void extend(Formatter.Builder formatterBuilder) {
    formatterBuilder.nodeFormatterFactory(TaskListNodeFormatter::new);
  }

  @Override
  public void rendererOptions(MutableDataHolder options) {}

  @Override
  public void parserOptions(MutableDataHolder options) {
    ListOptions.addItemMarkerSuffixes(options, "[ ]", "[x]", "[X]");
  }

  @Override
  public void extend(Parser.Builder parserBuilder) {
    parserBuilder.blockPreProcessorFactory(new TaskListItemBlockPreProcessor.Factory());
  }

  @Override
  public void extend(HtmlRenderer.Builder htmlRendererBuilder, String rendererType) {
    if (htmlRendererBuilder.isRendererType("HTML")) {
      htmlRendererBuilder.nodeRendererFactory(new TaskListNodeRenderer.Factory());
    }
  }
}
