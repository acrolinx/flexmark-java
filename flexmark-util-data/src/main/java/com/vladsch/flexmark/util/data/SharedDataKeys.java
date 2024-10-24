package com.vladsch.flexmark.util.data;

import com.vladsch.flexmark.util.misc.Extension;
import java.util.Collection;

public final class SharedDataKeys {
  // BuilderBase
  public static final DataKey<Collection<Extension>> EXTENSIONS =
      new DataKey<>("EXTENSIONS", Extension.EMPTY_LIST);

  // Parser
  public static final DataKey<Boolean> HEADING_NO_ATX_SPACE =
      new DataKey<>("HEADING_NO_ATX_SPACE", false);
  // used to set escaping of # at start independent of HEADING_NO_ATX_SPACE setting if desired
  public static final DataKey<Boolean> ESCAPE_HEADING_NO_ATX_SPACE =
      new DataKey<>("ESCAPE_HEADING_NO_ATX_SPACE", false, HEADING_NO_ATX_SPACE::get);
  public static final DataKey<Boolean> HTML_FOR_TRANSLATOR =
      new DataKey<>("HTML_FOR_TRANSLATOR", false);
  public static final DataKey<Boolean> INTELLIJ_DUMMY_IDENTIFIER =
      new DataKey<>("INTELLIJ_DUMMY_IDENTIFIER", false);
  public static final DataKey<Boolean> PARSE_INNER_HTML_COMMENTS =
      new DataKey<>("PARSE_INNER_HTML_COMMENTS", false);
  public static final DataKey<Boolean> BLANK_LINES_IN_AST =
      new DataKey<>("BLANK_LINES_IN_AST", false);
  public static final DataKey<String> TRANSLATION_HTML_BLOCK_TAG_PATTERN =
      new DataKey<>("TRANSLATION_HTML_BLOCK_TAG_PATTERN", "___(?:\\d+)_");
  public static final DataKey<String> TRANSLATION_HTML_INLINE_TAG_PATTERN =
      new DataKey<>("TRANSLATION_HTML_INLINE_TAG_PATTERN", "__(?:\\d+)_");
  public static final DataKey<String> TRANSLATION_AUTOLINK_TAG_PATTERN =
      new DataKey<>("TRANSLATION_AUTOLINK_TAG_PATTERN", "____(?:\\d+)_");

  public static final DataKey<Integer> RENDERER_MAX_TRAILING_BLANK_LINES =
      new DataKey<>("RENDERER_MAX_TRAILING_BLANK_LINES", 1);
  public static final DataKey<Integer> RENDERER_MAX_BLANK_LINES =
      new DataKey<>("RENDERER_MAX_BLANK_LINES", 1);
  public static final DataKey<Integer> INDENT_SIZE = new DataKey<>("INDENT_SIZE", 0);
  public static final DataKey<Boolean> PERCENT_ENCODE_URLS =
      new DataKey<>("PERCENT_ENCODE_URLS", false);
  public static final DataKey<Boolean> HEADER_ID_GENERATOR_RESOLVE_DUPES =
      new DataKey<>("HEADER_ID_GENERATOR_RESOLVE_DUPES", true);
  public static final DataKey<String> HEADER_ID_GENERATOR_TO_DASH_CHARS =
      new DataKey<>("HEADER_ID_GENERATOR_TO_DASH_CHARS", " -_");
  public static final DataKey<String> HEADER_ID_GENERATOR_NON_DASH_CHARS =
      new DataKey<>("HEADER_ID_GENERATOR_NON_DASH_CHARS", "");
  public static final DataKey<Boolean> HEADER_ID_GENERATOR_NO_DUPED_DASHES =
      new DataKey<>("HEADER_ID_GENERATOR_NO_DUPED_DASHES", false);
  public static final DataKey<Boolean> HEADER_ID_GENERATOR_NON_ASCII_TO_LOWERCASE =
      new DataKey<>("HEADER_ID_GENERATOR_NON_ASCII_TO_LOWERCASE", true);
  public static final DataKey<Boolean> HEADER_ID_REF_TEXT_TRIM_LEADING_SPACES =
      new DataKey<>("HEADER_ID_REF_TEXT_TRIM_LEADING_SPACES", true);
  public static final DataKey<Boolean> HEADER_ID_REF_TEXT_TRIM_TRAILING_SPACES =
      new DataKey<>("HEADER_ID_REF_TEXT_TRIM_TRAILING_SPACES", true);
  public static final DataKey<Boolean> HEADER_ID_ADD_EMOJI_SHORTCUT =
      new DataKey<>("HEADER_ID_ADD_EMOJI_SHORTCUT", false);
  public static final DataKey<Boolean> RENDER_HEADER_ID = new DataKey<>("RENDER_HEADER_ID", false);
  public static final DataKey<Boolean> GENERATE_HEADER_ID =
      new DataKey<>("GENERATE_HEADER_ID", true);
  public static final DataKey<Boolean> DO_NOT_RENDER_LINKS =
      new DataKey<>("DO_NOT_RENDER_LINKS", false);

  // Formatter
  public static final DataKey<Integer> FORMATTER_MAX_BLANK_LINES =
      new DataKey<>("FORMATTER_MAX_BLANK_LINES", 2);
  public static final DataKey<Integer> FORMATTER_MAX_TRAILING_BLANK_LINES =
      new DataKey<>("FORMATTER_MAX_TRAILING_BLANK_LINES", 1);
  public static final DataKey<Boolean> BLOCK_QUOTE_BLANK_LINES =
      new DataKey<>("BLOCK_QUOTE_BLANK_LINES", true);

  public static final DataKey<Boolean> APPLY_SPECIAL_LEAD_IN_HANDLERS =
      new DataKey<>("APPLY_SPECIAL_LEAD_IN_HANDLERS", true);
  public static final DataKey<Boolean> ESCAPE_NUMBERED_LEAD_IN =
      new DataKey<>("ESCAPE_NUMBERED_LEAD_IN", true, APPLY_SPECIAL_LEAD_IN_HANDLERS::get);
  public static final DataKey<Boolean> RUNNING_TESTS = new DataKey<>("RUNNING_TESTS", false);

  private SharedDataKeys() {
    throw new IllegalStateException();
  }
}
