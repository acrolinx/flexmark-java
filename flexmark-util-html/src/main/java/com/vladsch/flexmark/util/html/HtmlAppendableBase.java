package com.vladsch.flexmark.util.html;

import com.vladsch.flexmark.util.misc.BitFieldSet;
import com.vladsch.flexmark.util.misc.Utils;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Escaping;
import com.vladsch.flexmark.util.sequence.LineAppendable;
import com.vladsch.flexmark.util.sequence.LineAppendableImpl;
import com.vladsch.flexmark.util.sequence.LineInfo;
import com.vladsch.flexmark.util.sequence.Options;
import com.vladsch.flexmark.util.sequence.RepeatedSequence;
import com.vladsch.flexmark.util.sequence.builder.ISequenceBuilder;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

public class HtmlAppendableBase<T extends HtmlAppendableBase<T>> implements HtmlAppendable {
  private final LineAppendable appendable;

  private MutableAttributes currentAttributes;
  private boolean indentOnFirstEol = false;
  private boolean lineOnChildText = false;
  private boolean withAttributes = false;
  private boolean suppressOpenTagLine = false;
  private boolean suppressCloseTagLine = false;
  private final Stack<String> openTags = new Stack<>();

  public HtmlAppendableBase(LineAppendable other, boolean inheritIndent) {
    this(other, inheritIndent ? other.getIndentPrefix().length() : 0, other.getOptions());
  }

  public HtmlAppendableBase(int indentSize, int formatOptions) {
    this(null, indentSize, formatOptions);
  }

  public HtmlAppendableBase(Appendable other, int indentSize, int formatOptions) {
    this.appendable = new LineAppendableImpl(other, formatOptions);
    this.appendable.setIndentPrefix(RepeatedSequence.repeatOf(" ", indentSize).toString());
  }

  @Override
  public HtmlAppendable getEmptyAppendable() {
    return new HtmlAppendableBase<>(
        appendable, appendable.getIndentPrefix().length(), appendable.getOptions());
  }

  public boolean isSuppressOpenTagLine() {
    return suppressOpenTagLine;
  }

  public void setSuppressOpenTagLine(boolean suppressOpenTagLine) {
    this.suppressOpenTagLine = suppressOpenTagLine;
  }

  public boolean isSuppressCloseTagLine() {
    return suppressCloseTagLine;
  }

  public T setSuppressCloseTagLine(boolean suppressCloseTagLine) {
    this.suppressCloseTagLine = suppressCloseTagLine;
    return (T) this;
  }

  @Override
  public String toString() {
    return appendable.toString();
  }

  @Override
  public T openPre() {
    appendable.openPreFormatted(true);
    return (T) this;
  }

  @Override
  public T closePre() {
    appendable.closePreFormatted();
    return (T) this;
  }

  @Override
  public boolean inPre() {
    return appendable.isPreFormatted();
  }

  @Override
  public T raw(CharSequence s) {
    appendable.append(s);
    return (T) this;
  }

  @Override
  public T raw(CharSequence s, int count) {
    int i = count;
    while (i-- > 0) appendable.append(s);
    return (T) this;
  }

  @Override
  public T rawPre(CharSequence s) {
    // if previous pre-formatted did not have an EOL and this one does, need to transfer the EOL
    // to previous pre-formatted to have proper handling of first/last line, otherwise this opening
    // pre-formatted, blows away previous last line pre-formatted information
    if (appendable.getPendingEOL() == 0 && s.length() > 0 && s.charAt(0) == '\n') {
      appendable.line();
      s = s.subSequence(1, s.length());
    }

    appendable.openPreFormatted(true).append(s).closePreFormatted();
    return (T) this;
  }

  @Override
  public T rawIndentedPre(CharSequence s) {
    appendable.openPreFormatted(true).append(s).closePreFormatted();
    return (T) this;
  }

  @Override
  public T text(CharSequence s) {
    appendable.append(Escaping.escapeHtml(s, false));
    return (T) this;
  }

  @Override
  public T attr(CharSequence attrName, CharSequence value) {
    if (currentAttributes == null) {
      currentAttributes = new MutableAttributes();
    }
    currentAttributes.addValue(attrName, value);
    return (T) this;
  }

  @Override
  public T attr(Attribute... attribute) {
    if (currentAttributes == null) {
      currentAttributes = new MutableAttributes();
    }
    for (Attribute attr : attribute) {
      currentAttributes.addValue(attr.getName(), attr.getValue());
    }
    return (T) this;
  }

  @Override
  public T attr(Attributes attributes) {
    if (!attributes.isEmpty()) {
      if (currentAttributes == null) {
        currentAttributes = new MutableAttributes(attributes);
      } else {
        currentAttributes.addValues(attributes);
      }
    }
    return (T) this;
  }

  @Override
  public T withAttr() {
    withAttributes = true;
    return (T) this;
  }

  @Override
  public Attributes getAttributes() {
    return currentAttributes;
  }

  @Override
  public T setAttributes(Attributes attributes) {
    currentAttributes = attributes.toMutable();
    return (T) this;
  }

  @Override
  public T withCondLineOnChildText() {
    lineOnChildText = true;
    return (T) this;
  }

  @Override
  public T withCondIndent() {
    indentOnFirstEol = true;
    return (T) this;
  }

  @Override
  public T tag(CharSequence tagName) {
    return tag(tagName, false);
  }

  @Override
  public T tag(CharSequence tagName, Runnable runnable) {
    return tag(tagName, false, false, runnable);
  }

  @Override
  public T tagVoid(CharSequence tagName) {
    return tag(tagName, true);
  }

  private String getOpenTagText() {
    return Utils.splice(openTags, ", ", true);
  }

  private void pushTag(CharSequence tagName) {
    openTags.push(String.valueOf(tagName));
  }

  private void popTag(CharSequence tagName) {
    if (openTags.isEmpty())
      throw new IllegalStateException("Close tag '" + tagName + "' with no tags open");
    String openTag = openTags.peek();
    if (!openTag.equals(String.valueOf(tagName)))
      throw new IllegalStateException(
          "Close tag '" + tagName + "' does not match '" + openTag + "' in " + getOpenTagText());
    openTags.pop();
  }

  private void tagOpened(CharSequence tagName) {
    pushTag(tagName);
  }

  private void tagClosed(CharSequence tagName) {
    popTag(tagName);
  }

  @Override
  public Stack<String> getOpenTags() {
    return openTags;
  }

  @Override
  public List<String> getOpenTagsAfterLast(CharSequence latestTag) {
    if (openTags.isEmpty()) {
      return Collections.emptyList();
    }

    List<String> tagList = new ArrayList<>(openTags);
    int iMax = tagList.size();
    int lastPos = iMax;
    String lastTag = String.valueOf(latestTag);
    for (int i = iMax; i-- > 0; ) {
      if (tagList.get(i).equals(lastTag)) {
        lastPos = i + 1;
        break;
      }
    }
    return tagList.subList(lastPos, iMax);
  }

  @Override
  public T tag(CharSequence tagName, boolean voidElement) {
    if (tagName.length() == 0 || tagName.charAt(0) == '/') {
      return closeTag(tagName);
    }

    Attributes attributes = null;

    if (withAttributes) {
      attributes = currentAttributes;
      currentAttributes = null;
      withAttributes = false;
    }

    appendable.append("<");
    appendable.append(tagName);

    if (attributes != null && !attributes.isEmpty()) {
      for (Attribute attribute : attributes.values()) {
        CharSequence attributeValue = attribute.getValue();

        if (attribute.isNonRendering()) {
          continue;
        }

        appendable.append(" ");
        appendable.append(Escaping.escapeHtml(attribute.getName(), true));
        appendable.append("=\"");
        appendable.append(Escaping.escapeHtml(attributeValue, true));
        appendable.append("\"");
      }
    }

    if (voidElement) {
      appendable.append(" />");
    } else {
      appendable.append(">");
      tagOpened(tagName);
    }

    return (T) this;
  }

  @Override
  public T closeTag(CharSequence tagName) {
    if (tagName.length() == 0)
      throw new IllegalStateException("closeTag called with tag:'" + tagName + "'");

    if (tagName.charAt(0) == '/') {
      appendable.append("<").append(tagName).append(">");
      tagClosed(tagName.subSequence(1, tagName.length()));
    } else {
      appendable.append("</").append(tagName).append(">");
      tagClosed(tagName);
    }
    return (T) this;
  }

  @Override
  public T tag(CharSequence tagName, boolean withIndent, boolean withLine, Runnable runnable) {
    boolean isLineOnChildText = lineOnChildText;
    boolean isIndentOnFirstEol = indentOnFirstEol;
    lineOnChildText = false;
    indentOnFirstEol = false;

    if (withIndent && !suppressOpenTagLine) {
      appendable.line();
    }

    tag(tagName, false);

    if (withIndent && !isIndentOnFirstEol) appendable.indent();

    if ((appendable.getOptions() & F_PASS_THROUGH) != 0) {
      runnable.run();
    } else {
      boolean[] hadConditionalIndent = new boolean[] {false};
      Runnable indentOnFirstEol = () -> hadConditionalIndent[0] = true;

      if (isLineOnChildText) appendable.setLineOnFirstText();

      if (isIndentOnFirstEol) {
        appendable.addIndentOnFirstEOL(indentOnFirstEol);
      }

      runnable.run();

      if (isLineOnChildText) appendable.clearLineOnFirstText();

      if (hadConditionalIndent[0]) {
        appendable.unIndentNoEol();
      } else {
        appendable.removeIndentOnFirstEOL(indentOnFirstEol);
      }
    }

    if (withIndent && !isIndentOnFirstEol) appendable.unIndent();

    // don't rely on unIndent() doing a line, it will only do so if there was text since indent()
    if (withLine && !suppressCloseTagLine) appendable.line();

    closeTag(tagName);

    if (withIndent && !suppressCloseTagLine) {
      appendable.line();
    }

    return (T) this;
  }

  @Override
  public T tagVoidLine(CharSequence tagName) {
    lineIf(!suppressOpenTagLine).tagVoid(tagName).lineIf(!suppressCloseTagLine);
    return (T) this;
  }

  @Override
  public T tagLine(CharSequence tagName) {
    lineIf(!suppressOpenTagLine).tag(tagName).lineIf(!suppressCloseTagLine);
    return (T) this;
  }

  @Override
  public T tagLine(CharSequence tagName, boolean voidElement) {
    lineIf(!suppressOpenTagLine).tag(tagName, voidElement).lineIf(!suppressCloseTagLine);
    return (T) this;
  }

  @Override
  public T tagLine(CharSequence tagName, Runnable runnable) {
    lineIf(!suppressOpenTagLine).tag(tagName, false, false, runnable).lineIf(!suppressCloseTagLine);
    return (T) this;
  }

  @Override
  public T tagIndent(CharSequence tagName, Runnable runnable) {
    tag(tagName, true, false, runnable);
    return (T) this;
  }

  @Override
  public T tagLineIndent(CharSequence tagName, Runnable runnable) {
    tag(tagName, true, true, runnable);
    return (T) this;
  }

  // delegated to LineFormattingAppendable

  @Override
  public Iterator<LineInfo> iterator() {
    return appendable.iterator();
  }

  @Override
  public Iterable<BasedSequence> getLines(
      int maxTrailingBlankLines, int startLine, int endLine, boolean withPrefixes) {
    return appendable.getLines(maxTrailingBlankLines, startLine, endLine, true);
  }

  @Override
  public Iterable<LineInfo> getLinesInfo(int maxTrailingBlankLines, int startLine, int endLine) {
    return appendable.getLinesInfo(maxTrailingBlankLines, startLine, endLine);
  }

  @Override
  public void setPrefixLength(int lineIndex, int prefixEndIndex) {
    appendable.setPrefixLength(lineIndex, prefixEndIndex);
  }

  @Override
  public void insertLine(int lineIndex, CharSequence prefix, CharSequence text) {
    appendable.insertLine(lineIndex, prefix, text);
  }

  @Override
  public void setLine(int lineIndex, CharSequence prefix, CharSequence text) {
    appendable.setLine(lineIndex, prefix, text);
  }

  @Override
  public <T extends Appendable> T appendTo(
      T out,
      boolean withPrefixes,
      int maxBlankLines,
      int maxTrailingBlankLines,
      int startLine,
      int endLine)
      throws IOException {
    return appendable.appendTo(
        out, withPrefixes, maxBlankLines, maxTrailingBlankLines, startLine, endLine);
  }

  @Override
  public boolean endsWithEOL() {
    return appendable.endsWithEOL();
  }

  @Override
  public boolean isPendingSpace() {
    return appendable.isPendingSpace();
  }

  @Override
  public boolean isPreFormatted() {
    return appendable.isPreFormatted();
  }

  @Override
  public int getTrailingBlankLines(int endLine) {
    return appendable.getTrailingBlankLines(endLine);
  }

  @Override
  public int column() {
    return appendable.column();
  }

  @Override
  public int getLineCount() {
    return appendable.getLineCount();
  }

  @Override
  public int getLineCountWithPending() {
    return appendable.getLineCountWithPending();
  }

  @Override
  public int getOptions() {
    return appendable.getOptions();
  }

  @Override
  public int getPendingSpace() {
    return appendable.getPendingSpace();
  }

  @Override
  public int getPendingEOL() {
    return appendable.getPendingEOL();
  }

  @Override
  public int offset() {
    return appendable.offset();
  }

  @Override
  public int offsetWithPending() {
    return appendable.offsetWithPending();
  }

  @Override
  public int getAfterEolPrefixDelta() {
    return appendable.getAfterEolPrefixDelta();
  }

  @Override
  public ISequenceBuilder<?, ?> getBuilder() {
    return appendable.getBuilder();
  }

  @Override
  public BasedSequence getPrefix() {
    return appendable.getPrefix();
  }

  @Override
  public BasedSequence getBeforeEolPrefix() {
    return appendable.getBeforeEolPrefix();
  }

  @Override
  public LineInfo getLineInfo(int lineIndex) {
    return appendable.getLineInfo(lineIndex);
  }

  @Override
  public BasedSequence getLine(int lineIndex) {
    return appendable.getLine(lineIndex);
  }

  @Override
  public BasedSequence getIndentPrefix() {
    return appendable.getIndentPrefix();
  }

  @Override
  public CharSequence toSequence(
      int maxBlankLines, int maxTrailingBlankLines, boolean withPrefixes) {
    return appendable.toSequence(maxBlankLines, maxTrailingBlankLines, withPrefixes);
  }

  @Override
  public String toString(int maxBlankLines, int maxTrailingBlankLines, boolean withPrefixes) {
    return appendable.toString(maxBlankLines, maxTrailingBlankLines, withPrefixes);
  }

  @Override
  public BitFieldSet<Options> getOptionSet() {
    return appendable.getOptionSet();
  }

  @Override
  public T removeExtraBlankLines(
      int maxBlankLines, int maxTrailingBlankLines, int startLine, int endLine) {
    appendable.removeExtraBlankLines(maxBlankLines, maxTrailingBlankLines, startLine, endLine);
    return (T) this;
  }

  @Override
  public T removeLines(int startLine, int endLine) {
    appendable.removeLines(startLine, endLine);
    return (T) this;
  }

  @Override
  public T pushOptions() {
    appendable.pushOptions();
    return (T) this;
  }

  @Override
  public T popOptions() {
    appendable.popOptions();
    return (T) this;
  }

  @Override
  public T changeOptions(int addFlags, int removeFlags) {
    appendable.changeOptions(addFlags, removeFlags);
    return (T) this;
  }

  @Override
  public T addIndentOnFirstEOL(Runnable listener) {
    appendable.addIndentOnFirstEOL(listener);
    return (T) this;
  }

  @Override
  public T addPrefix(CharSequence prefix) {
    appendable.addPrefix(prefix);
    return (T) this;
  }

  @Override
  public T addPrefix(CharSequence prefix, boolean afterEol) {
    appendable.addPrefix(prefix, afterEol);
    return (T) this;
  }

  @Override
  public T append(char c) {
    appendable.append(c);
    return (T) this;
  }

  @Override
  public T append(CharSequence csq) {
    appendable.append(csq);
    return (T) this;
  }

  @Override
  public T append(CharSequence csq, int start, int end) {
    appendable.append(csq, start, end);
    return (T) this;
  }

  @Override
  public T append(LineAppendable lines, int startLine, int endLine, boolean withPrefixes) {
    appendable.append(lines, startLine, endLine, withPrefixes);
    return (T) this;
  }

  @Override
  public T blankLine() {
    appendable.blankLine();
    return (T) this;
  }

  @Override
  public T blankLine(int count) {
    appendable.blankLine(count);
    return (T) this;
  }

  @Override
  public T blankLineIf(boolean predicate) {
    appendable.blankLineIf(predicate);
    return (T) this;
  }

  @Override
  public T closePreFormatted() {
    appendable.closePreFormatted();
    return (T) this;
  }

  @Override
  public T indent() {
    appendable.indent();
    return (T) this;
  }

  @Override
  public T line() {
    appendable.line();
    return (T) this;
  }

  @Override
  public T lineIf(boolean predicate) {
    appendable.lineIf(predicate);
    return (T) this;
  }

  @Override
  public T lineOnFirstText(boolean value) {
    appendable.lineOnFirstText(value);
    return (T) this;
  }

  @Override
  public T lineWithTrailingSpaces(int count) {
    appendable.lineWithTrailingSpaces(count);
    return (T) this;
  }

  @Override
  public T openPreFormatted(boolean keepIndent) {
    appendable.openPreFormatted(keepIndent);
    return (T) this;
  }

  @Override
  public T popPrefix() {
    appendable.popPrefix();
    return (T) this;
  }

  @Override
  public T popPrefix(boolean afterEol) {
    appendable.popPrefix(afterEol);
    return (T) this;
  }

  @Override
  public T pushPrefix() {
    appendable.pushPrefix();
    return (T) this;
  }

  @Override
  public T removeIndentOnFirstEOL(Runnable listener) {
    appendable.removeIndentOnFirstEOL(listener);
    return (T) this;
  }

  @Override
  public T append(char c, int count) {
    appendable.append(c, count);
    return (T) this;
  }

  @Override
  public T setIndentPrefix(CharSequence prefix) {
    appendable.setIndentPrefix(prefix);
    return (T) this;
  }

  @Override
  public T setOptions(int flags) {
    appendable.setOptions(flags);
    return (T) this;
  }

  @Override
  public T setPrefix(CharSequence prefix) {
    appendable.setPrefix(prefix);
    return (T) this;
  }

  @Override
  public T setPrefix(CharSequence prefix, boolean afterEol) {
    appendable.setPrefix(prefix, afterEol);
    return (T) this;
  }

  @Override
  public T unIndent() {
    appendable.unIndent();
    return (T) this;
  }

  @Override
  public T unIndentNoEol() {
    appendable.unIndentNoEol();
    return (T) this;
  }
}
