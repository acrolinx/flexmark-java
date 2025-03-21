package com.vladsch.flexmark.util.sequence;

import com.vladsch.flexmark.util.misc.BitFieldSet;
import com.vladsch.flexmark.util.sequence.builder.ISequenceBuilder;
import java.io.IOException;
import java.util.Iterator;

/**
 * Used to collect line text for further processing
 *
 * <p>control output of new lines limiting them to terminate text but not create blank lines, and
 * control number of blank lines output, eliminate spaces before and after an \n, except in prefixes
 * and indents controlled by this class.
 *
 * <p>allows appending unmodified text in preformatted regions created by {@link
 * #openPreFormatted(boolean)} and {@link #closePreFormatted()}
 *
 * <p>consecutive \n in the data are going go be collapsed to a single \n. To get blank lines use
 * {@link #blankLine()} or {@link #blankLine(int)}
 *
 * <p>tab is converted to spaces if {@link #F_CONVERT_TABS} or {@link #F_COLLAPSE_WHITESPACE} option
 * is selected
 *
 * <p>spaces before and after \n are removed controlled by {@link #F_TRIM_TRAILING_WHITESPACE} and
 * {@link #F_TRIM_LEADING_WHITESPACE}
 *
 * <p>use {@link #line()}, {@link #lineIf(boolean)}, {@link #blankLine()} as an alternative to
 * appending \n. use {@link #blankLineIf(boolean)} and {@link #blankLine(int)} for appending blank
 * lines.
 */
public interface LineAppendable extends Appendable, Iterable<LineInfo> {
  Options O_CONVERT_TABS = Options.CONVERT_TABS;
  Options O_COLLAPSE_WHITESPACE = Options.COLLAPSE_WHITESPACE;
  Options O_TRIM_TRAILING_WHITESPACE = Options.TRIM_TRAILING_WHITESPACE;
  Options O_PASS_THROUGH = Options.PASS_THROUGH;
  Options O_TRIM_LEADING_WHITESPACE = Options.TRIM_LEADING_WHITESPACE;
  Options O_TRIM_LEADING_EOL = Options.TRIM_LEADING_EOL;
  Options O_PREFIX_PRE_FORMATTED = Options.PREFIX_PRE_FORMATTED;

  int F_CONVERT_TABS = BitFieldSet.intMask(O_CONVERT_TABS); // expand tabs on column multiples of 4
  int F_COLLAPSE_WHITESPACE =
      BitFieldSet.intMask(
          O_COLLAPSE_WHITESPACE); // collapse multiple tabs and spaces to single space
  int F_TRIM_TRAILING_WHITESPACE =
      BitFieldSet.intMask(O_TRIM_TRAILING_WHITESPACE); // don't output trailing whitespace
  int F_PASS_THROUGH =
      BitFieldSet.intMask(
          O_PASS_THROUGH); // just pass everything through to appendable with no formatting

  // NOTE: ALLOW_LEADING_WHITESPACE is now inverted and named F_TRIM_LEADING_WHITESPACE
  int F_TRIM_LEADING_WHITESPACE =
      BitFieldSet.intMask(O_TRIM_LEADING_WHITESPACE); // allow leading spaces on a line, else remove
  // NOTE: ALLOW_LEADING_EOL is now inverted and named F_TRIM_LEADING_EOL
  int F_TRIM_LEADING_EOL = BitFieldSet.intMask(O_TRIM_LEADING_EOL); // allow EOL at offset 0
  int F_PREFIX_PRE_FORMATTED =
      BitFieldSet.intMask(
          O_PREFIX_PRE_FORMATTED); // when prefixing lines, prefix pre-formatted lines
  int F_FORMAT_ALL =
      F_CONVERT_TABS
          | F_COLLAPSE_WHITESPACE
          | F_TRIM_TRAILING_WHITESPACE
          | F_TRIM_LEADING_WHITESPACE
          | F_TRIM_LEADING_EOL; // select all formatting options

  int F_WHITESPACE_REMOVAL =
      LineAppendable.F_COLLAPSE_WHITESPACE
          | LineAppendable.F_TRIM_TRAILING_WHITESPACE
          | LineAppendable.F_TRIM_LEADING_WHITESPACE;

  static BitFieldSet<Options> toOptionSet(int options) {
    return BitFieldSet.of(Options.class, options);
  }

  /**
   * Get current options as bit mask flags
   *
   * @return option flags
   */
  default int getOptions() {
    return getOptionSet().toInt();
  }

  LineAppendable getEmptyAppendable();

  /**
   * Get current options as set which can be used to modify options
   *
   * @return mutable option set
   */
  BitFieldSet<Options> getOptionSet();

  LineAppendable pushOptions();

  LineAppendable popOptions();

  default LineAppendable preserveSpaces() {
    return changeOptions(0, F_TRIM_LEADING_WHITESPACE | F_COLLAPSE_WHITESPACE);
  }

  default LineAppendable removeOptions(int flags) {
    return changeOptions(0, flags);
  }

  LineAppendable changeOptions(int addFlags, int removeFlags);

  /**
   * Set options on processing text
   *
   * @param flags option flags
   * @return this
   */
  default LineAppendable setOptions(int flags) {
    return setOptions(toOptionSet(flags));
  }

  /**
   * Set options on processing text
   *
   * @param options option set
   * @return this
   */
  default LineAppendable setOptions(BitFieldSet<Options> options) {
    return setOptions(options.toInt());
  }

  /**
   * Get builder used for accumulation
   *
   * @return builder used for accumulation
   */
  ISequenceBuilder<?, ?> getBuilder();

  /**
   * Get trailing blank line count ending on given line
   *
   * @param endLine end line
   * @return number of trailing blank lines
   */
  int getTrailingBlankLines(int endLine);

  /**
   * @return true if not empty and have no unterminated lines
   */
  boolean endsWithEOL();

  // these methods are monitored for content and formatting applied
  @Override
  LineAppendable append(CharSequence csq);

  @Override
  LineAppendable append(CharSequence csq, int start, int end);

  @Override
  LineAppendable append(char c);

  LineAppendable append(char c, int count);

  /**
   * Append lines from another line formatting appendable.
   *
   * <p>NOTE: does not apply formatting options. Instead, appends already formatted lines as is
   *
   * <p>If there is an accumulating line, it will be terminated by an EOL before appending lines
   *
   * @param lineAppendable lines to append, any unterminated lines will be terminated by a {@link
   *     #line()} invocation.
   * @param startLine start line to append
   * @param endLine end line to append, endLine is excluded.
   * @param withPrefixes true if to include prefixes from the lineAppendable.
   * @return this
   */
  LineAppendable append(
      LineAppendable lineAppendable, int startLine, int endLine, boolean withPrefixes);

  /**
   * Append lines from another line formatting appendable.
   *
   * <p>NOTE: does not apply formatting options other than prefix. Line text is used as is.
   *
   * <p>If there is an unterminated line its contents will be used as leading text of the first
   * appended line
   *
   * @param lineAppendable lines to append
   * @return this
   */
  default LineAppendable append(LineAppendable lineAppendable) {
    return append(lineAppendable, 0, Integer.MAX_VALUE, true);
  }

  /**
   * Append lines from another line formatting appendable.
   *
   * <p>NOTE: does not apply formatting options other than prefix. Line text is used as is.
   *
   * <p>If there is an unterminated line its contents will be used as leading text of the first
   * appended line
   *
   * @param lineAppendable lines to append
   * @param withPrefixes true if to include prefixes from the lineAppendable.
   * @return this
   */
  default LineAppendable append(LineAppendable lineAppendable, boolean withPrefixes) {
    return append(lineAppendable, 0, Integer.MAX_VALUE, withPrefixes);
  }

  /**
   * Add a new line if there was any unterminated text appended or if this is a preformatted region
   *
   * @return this
   */
  LineAppendable line();

  /**
   * Add a new line, keep trailing spaces if there was any unterminated text appended
   *
   * <p>NOTE: only applies in preformatted region or if the line is not empty
   *
   * @param count number of trailing spaces to add
   * @return this
   */
  LineAppendable lineWithTrailingSpaces(int count);

  /**
   * Add a new line, if predicate is true and line() would add an EOL.
   *
   * @param predicate call {@link #line()} if value is true.
   * @return this
   */
  LineAppendable lineIf(boolean predicate);

  /**
   * Add a blank line, if there is not one already appended.
   *
   * @return this
   */
  LineAppendable blankLine();

  /**
   * Add a blank line, if predicate is true and there isn't already blank lines appended.
   *
   * @param predicate when true append blank line
   * @return this
   */
  LineAppendable blankLineIf(boolean predicate);

  /**
   * Add a blank lines, if there isn't already given number of blank lines appended. Will append
   * only enough blank lines to increase it to given level. If more are already in the wings then
   * nothing is done.
   *
   * @param count number of blank lines to append
   * @return this
   */
  LineAppendable blankLine(int count);

  /**
   * @return true if in pre-formatted region
   */
  boolean isPreFormatted();

  /**
   * Open preformatted section and suspend content modification
   *
   * @param addPrefixToFirstLine if true will add the current prefix to first line
   * @return this
   */
  LineAppendable openPreFormatted(boolean addPrefixToFirstLine);

  /**
   * Close preformatted section and suspend content modification
   *
   * @return this
   */
  LineAppendable closePreFormatted();

  /**
   * Increase the indent level, will terminate the current line if there is unterminated text
   *
   * <p>NOTE: this is equivalent to pushPrefix(), addPrefix(getIndentPrefix()) but adds a flag to
   * validate that {@link #unIndent()} is called only on prefixes added by this method
   *
   * @return this
   */
  LineAppendable indent();

  /**
   * Decrease the indent level, min level is 0, will terminate the current line if there is
   * unterminated text
   *
   * <p>NOTE: this is equivalent to popPrefix() but with validation that it is called only on
   * prefixes added by {@link #indent()}
   *
   * @return this
   */
  LineAppendable unIndent();

  /**
   * Decrease the indent level, if there is unterminated text then unindented prefix is to be
   * applied after the next EOL.
   *
   * <p>Will NOT terminate the current line if there is unterminated text
   *
   * <p>NOTE: should be used with {@link #addIndentOnFirstEOL(Runnable)} if callback is invoked
   *
   * @return this
   */
  LineAppendable unIndentNoEol();

  /**
   * Get prefix appended after a new line character for every indent level
   *
   * @return char sequence of the current indent prefix used for each indent level
   */
  BasedSequence getIndentPrefix();

  /**
   * Set prefix to append after a new line character for every indent level
   *
   * @param prefix prefix characters for new lines appended after this is set
   * @return this
   */
  LineAppendable setIndentPrefix(CharSequence prefix);

  /**
   * Get prefix being applied to all lines, even in pre-formatted sections This is the prefix that
   * will be set after EOL
   *
   * @return char sequence of the current prefix
   */
  BasedSequence getPrefix();

  /**
   * Get prefix used before EOL
   *
   * @return char sequence of the current prefix
   */
  BasedSequence getBeforeEolPrefix();

  /**
   * Add to prefix appended after a new line character for every line and after a new line in
   * pre-formatted sections
   *
   * <p>This appends the sequence to current prefix
   *
   * @param prefix prefix characters to add to current prefix for new lines appended after this is
   *     set
   * @param afterEol if true prefix will take effect after EOL
   * @return this
   */
  LineAppendable addPrefix(CharSequence prefix, boolean afterEol);

  /**
   * Set prefix appended after a new line character for every line and after a new line in
   * pre-formatted sections
   *
   * <p>This appends the sequence to current prefix
   *
   * @param prefix prefix characters to add to current prefix for new lines appended after this is
   *     set
   * @param afterEol if true prefix will take effect after EOL
   * @return this
   */
  LineAppendable setPrefix(CharSequence prefix, boolean afterEol);

  /**
   * Add to prefix appended after a new line character for every line and after a new line in
   * pre-formatted sections
   *
   * <p>This appends the sequence to current prefix
   *
   * @param prefix prefix characters to add to current prefix for new lines appended after this is
   *     set
   * @return this
   */
  default LineAppendable addPrefix(CharSequence prefix) {
    return addPrefix(prefix, getPendingEOL() == 0);
  }

  /**
   * Set prefix appended after a new line character for every line and after a new line in
   * pre-formatted sections
   *
   * <p>This appends the sequence to current prefix
   *
   * @param prefix prefix characters to add to current prefix for new lines appended after this is
   *     set
   * @return this
   */
  default LineAppendable setPrefix(CharSequence prefix) {
    return setPrefix(prefix, getPendingEOL() == 0);
  }

  /**
   * Save the current prefix on the stack
   *
   * @return this
   */
  LineAppendable pushPrefix();

  /**
   * Pop a prefix from the stack and set the current prefix
   *
   * @param afterEol if true prefix will take effect after EOL
   * @return this
   */
  LineAppendable popPrefix(boolean afterEol);

  /**
   * Pop a prefix from the stack and set the current prefix
   *
   * @return this
   */
  default LineAppendable popPrefix() {
    return popPrefix(false);
  }

  /**
   * Get pending prefix after EOL
   *
   * @return change in prefix length after next eol
   */
  int getAfterEolPrefixDelta();

  /**
   * Get column offset after last append
   *
   * @return column offset after last append
   */
  int column();

  /**
   * Get text offset of all output lines, excluding any text for the last line being accumulated
   *
   * @return offset of text as would be returned for all
   */
  int offset();

  /**
   * Get offset after last append as if EOL was added but without the EOL itself
   *
   * @return offset as would be returned by {@link #offset()} after line() call less 1 for EOL
   */
  int offsetWithPending();

  /**
   * Test if trailing text ends in space or tab
   *
   * @return true if ending in space or tab
   */
  boolean isPendingSpace();

  /**
   * Get number of spaces at end of pending text
   *
   * @return number of eols at end of text
   */
  int getPendingSpace();

  /**
   * Get number of EOLs at end of appendable, this is actually number of tail blank lines
   *
   * @return number of eols at end of text
   */
  int getPendingEOL();

  LineAppendable lineOnFirstText(boolean value);

  default LineAppendable setLineOnFirstText() {
    return lineOnFirstText(true);
  }

  default LineAppendable clearLineOnFirstText() {
    return lineOnFirstText(false);
  }

  /**
   * Add an indent on first EOL appended and run runnable
   *
   * @param listener runnable to run if adding indent on first EOL
   * @return this
   */
  LineAppendable addIndentOnFirstEOL(Runnable listener);

  /**
   * Remove runnable, has no effect if EOL was already appended and runnable was run
   *
   * @param listener runnable added with addIndentOnFirstEOL
   * @return this
   */
  LineAppendable removeIndentOnFirstEOL(Runnable listener);

  /**
   * Get the number of lines appended, not including any unterminated ones
   *
   * @return number of full lines appended
   */
  int getLineCount();

  /**
   * Get the number of lines appended, including any unterminated ones
   *
   * <p>NOTE: if there is an unterminated line it will be available as the last line, without being
   * terminated explicitly
   *
   * @return number of lines appended
   */
  int getLineCountWithPending();

  /**
   * Get Line information at given line index
   *
   * <p>NOTE: if there is an unterminated line it will be available as the last line, without being
   * terminated explicitly
   *
   * @param lineIndex line index for the info to get
   * @return line info
   */
  LineInfo getLineInfo(int lineIndex);

  /**
   * Kotlin index operator
   *
   * @param lineIndex line index
   * @return line info
   */
  default LineInfo get(int lineIndex) {
    return getLineInfo(lineIndex);
  }

  /**
   * Get Line at given line index
   *
   * <p>NOTE: if there is an unterminated line it will be available as the last line, without being
   * terminated explicitly
   *
   * @param lineIndex line index
   * @return line char sequence
   */
  BasedSequence getLine(int lineIndex);

  /**
   * Full line iterator NOTE: will not issue line() to terminate any unterminated lines before
   * iteration and will not include unterminated lines in iteration
   *
   * @return iterator over lines
   */
  @Override
  Iterator<LineInfo> iterator();

  /**
   * Full line iterator over some lines
   *
   * <p>NOTE: will issue line() to terminate any unterminated lines before iteration
   *
   * @param maxTrailingBlankLines maximum trailing blank lines, -1 if trailing EOL should be removed
   * @param startLine start line index
   * @param endLine end line index, exclusive
   * @param withPrefixes true if prefixes should be included, else only non-prefix line text
   * @return iterator over lines
   */
  Iterable<BasedSequence> getLines(
      int maxTrailingBlankLines, int startLine, int endLine, boolean withPrefixes);

  default Iterable<BasedSequence> getLines(int maxTrailingBlankLines) {
    return getLines(maxTrailingBlankLines, 0, Integer.MAX_VALUE, true);
  }

  default Iterable<BasedSequence> getLines(int maxTrailingBlankLines, boolean withPrefixes) {
    return getLines(maxTrailingBlankLines, 0, Integer.MAX_VALUE, withPrefixes);
  }

  /**
   * Full line iterator with line info
   *
   * <p>NOTE: will issue line() to terminate any unterminated lines before iteration
   *
   * @param maxTrailingBlankLines maximum trailing blank lines, -1 if trailing EOL should be removed
   * @param startLine start line index
   * @param endLine end line index, exclusive
   * @return iterator over lines
   */
  Iterable<LineInfo> getLinesInfo(int maxTrailingBlankLines, int startLine, int endLine);

  default Iterable<LineInfo> getLinesInfo(int maxTrailingBlankLines) {
    return getLinesInfo(maxTrailingBlankLines, 0, Integer.MAX_VALUE);
  }

  /**
   * Change prefix length for a given line without changing the line content
   *
   * @param lineIndex index of the line
   * @param prefixLength new prefix length
   */
  void setPrefixLength(int lineIndex, int prefixLength);

  /**
   * Set content and prefix for a line
   *
   * @param lineIndex index of the line
   * @param prefix prefix of the line
   * @param text content text of the line
   */
  void setLine(int lineIndex, CharSequence prefix, CharSequence text);

  /**
   * Insert a line at the index with given content and prefix for a line
   *
   * @param lineIndex index of the line
   * @param prefix prefix of the line
   * @param text content text of the line
   */
  void insertLine(int lineIndex, CharSequence prefix, CharSequence text);

  LineAppendable removeLines(int startLine, int endLine);

  /**
   * get the resulting text for all lines
   *
   * @param maxBlankLines maximum blank lines to allow in the text
   * @param maxTrailingBlankLines maximum trailing blank lines
   * @param withPrefixes true if to include prefixes
   * @return resulting text
   */
  String toString(int maxBlankLines, int maxTrailingBlankLines, boolean withPrefixes);

  default String toString(int maxBlankLines, int maxTrailingBlankLines) {
    return toString(maxBlankLines, maxTrailingBlankLines, true);
  }

  /**
   * get the resulting text for all lines
   *
   * @param maxBlankLines maximum blank lines to allow in the text
   * @param maxTrailingBlankLines maximum trailing blank lines
   * @param withPrefixes true if to include prefixes
   * @return resulting text
   */
  CharSequence toSequence(int maxBlankLines, int maxTrailingBlankLines, boolean withPrefixes);

  default CharSequence toSequence(int maxBlankLines, int maxTrailingBlankLines) {
    return toSequence(maxBlankLines, maxTrailingBlankLines, true);
  }

  default CharSequence toSequence() {
    return toSequence(Integer.MAX_VALUE, Integer.MAX_VALUE, true);
  }

  /**
   * append lines to appendable with given maximum trailing blank lines and given prefix to add to
   * all lines
   *
   * <p>NOTE:
   *
   * @param <T> type of out
   * @param out appendable to output the resulting lines
   * @param withPrefixes true if to include prefixes
   * @param maxBlankLines maximum blank lines to allow in the body,
   * @param maxTrailingBlankLines maximum trailing blank lines at the end, if &lt;maxBlankLines then
   *     maxBlankLines will be used, if -1 then no trailing EOL will be added
   * @param startLine line from which to start output
   * @param endLine line at which to stop output
   * @return out
   * @throws IOException if thrown by appendable
   */
  <T extends Appendable> T appendTo(
      T out,
      boolean withPrefixes,
      int maxBlankLines,
      int maxTrailingBlankLines,
      int startLine,
      int endLine)
      throws IOException;

  default <T extends Appendable> T appendTo(
      T out, int maxBlankLines, int maxTrailingBlankLines, int startLine, int endLine)
      throws IOException {
    return appendTo(out, true, maxBlankLines, maxTrailingBlankLines, startLine, endLine);
  }

  default <T extends Appendable> T appendTo(T out, int maxBlankLines, int maxTrailingBlankLines)
      throws IOException {
    return appendTo(out, maxBlankLines, maxTrailingBlankLines, 0, Integer.MAX_VALUE);
  }

  /**
   * append lines to appendable with 0 blank lines, if these are desired at the end of the output
   * use {@link #appendTo(Appendable, int, int)}.
   *
   * @param <T> type of out
   * @param out appendable to output the resulting lines
   * @return out
   * @throws IOException thrown by {@code out}.
   */
  default <T extends Appendable> T appendTo(T out) throws IOException {
    return appendTo(out, 0, 0, 0, Integer.MAX_VALUE);
  }

  default <T extends Appendable> T appendToSilently(
      T out,
      boolean withPrefixes,
      int maxBlankLines,
      int maxTrailingBlankLines,
      int startLine,
      int endLine) {
    try {
      appendTo(out, withPrefixes, maxBlankLines, maxTrailingBlankLines, startLine, endLine);
    } catch (IOException ignored) {
    }
    return out;
  }

  default <T extends Appendable> T appendToSilently(
      T out, int maxBlankLines, int maxTrailingBlankLines, int startLine, int endLine) {
    appendToSilently(out, true, maxBlankLines, maxTrailingBlankLines, startLine, endLine);
    return out;
  }

  default <T extends Appendable> T appendToSilently(
      T out, int maxBlankLines, int maxTrailingBlankLines) {
    appendToSilently(out, maxBlankLines, maxTrailingBlankLines, 0, Integer.MAX_VALUE);
    return out;
  }

  /**
   * Normalize the appendable by removing extra blank lines in the body or at the end of given line
   * range
   *
   * @param maxBlankLines maximum blank lines to allow in the body
   * @param maxTrailingBlankLines maximum trailing blank lines ending on endLine, if
   *     &lt;maxBlankLines then maxBlankLines will be used
   * @param startLine line from which to start output
   * @param endLine line at which to stop output
   * @return this
   */
  LineAppendable removeExtraBlankLines(
      int maxBlankLines, int maxTrailingBlankLines, int startLine, int endLine);

  default LineAppendable removeExtraBlankLines(int maxBlankLines, int maxTrailingBlankLines) {
    return removeExtraBlankLines(maxBlankLines, maxTrailingBlankLines, 0, Integer.MAX_VALUE);
  }

  static CharSequence combinedPrefix(CharSequence prefix, CharSequence suffix) {
    if (prefix != null && prefix.length() > 0 && suffix != null && suffix.length() > 0) {
      return String.valueOf(prefix) + suffix;
    } else if (prefix != null && prefix.length() > 0) {
      return prefix;
    } else if (suffix != null && suffix.length() > 0) {
      return suffix;
    } else {
      return BasedSequence.NULL;
    }
  }
}
