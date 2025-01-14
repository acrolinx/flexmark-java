package com.vladsch.flexmark.util.sequence;

import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKeyBase;

/**
 * A CharSequence that references original char[] a subSequence() returns a sub-sequence from the
 * original base sequence
 */
public final class CharSubSequence extends BasedSequenceImpl {
  private final char[] baseChars;
  private final CharSubSequence base;
  private final int startOffset;
  private final int endOffset;

  private CharSubSequence(char[] chars, int hash) {
    super(hash);

    base = this;
    baseChars = chars;
    startOffset = 0;
    endOffset = baseChars.length;
  }

  private CharSubSequence(CharSubSequence baseSeq, int startIndex, int endIndex) {
    super(0);

    base = baseSeq;
    baseChars = baseSeq.baseChars;
    startOffset = base.startOffset + startIndex;
    endOffset = base.startOffset + endIndex;
  }

  @Override
  public int getOptionFlags() {
    return 0;
  }

  @Override
  public boolean allOptions(int options) {
    return false;
  }

  @Override
  public boolean anyOptions(int options) {
    return false;
  }

  @Override
  public <T> T getOption(DataKeyBase<T> dataKey) {
    return dataKey.get(null);
  }

  @Override
  public DataHolder getOptions() {
    return null;
  }

  @Override
  public CharSubSequence getBaseSequence() {
    return base;
  }

  @Override
  public char[] getBase() {
    return baseChars;
  }

  @Override
  public int getStartOffset() {
    return startOffset;
  }

  @Override
  public int getEndOffset() {
    return endOffset;
  }

  @Override
  public int length() {
    return endOffset - startOffset;
  }

  @Override
  public Range getSourceRange() {
    return Range.of(startOffset, endOffset);
  }

  @Override
  public int getIndexOffset(int index) {
    SequenceUtils.validateIndexInclusiveEnd(index, length());
    return startOffset + index;
  }

  @Override
  public char charAt(int index) {
    SequenceUtils.validateIndex(index, length());
    char c = baseChars[index + startOffset];
    return c == SequenceUtils.NUL ? SequenceUtils.ENC_NUL : c;
  }

  @Override
  public CharSubSequence subSequence(int startIndex, int endIndex) {
    SequenceUtils.validateStartEnd(startIndex, endIndex, length());
    return base.baseSubSequence(startOffset + startIndex, startOffset + endIndex);
  }

  @Override
  public CharSubSequence baseSubSequence(int startIndex, int endIndex) {
    SequenceUtils.validateStartEnd(startIndex, endIndex, baseChars.length);
    return startIndex == startOffset && endIndex == endOffset
        ? this
        : base != this
            ? base.baseSubSequence(startIndex, endIndex)
            : new CharSubSequence(base, startIndex, endIndex);
  }

  public static CharSubSequence of(CharSequence charSequence) {
    return of(charSequence, 0, charSequence.length());
  }

  /**
   * @param charSequence char sequence
   * @param startIndex start index in sequence
   * @param endIndex end index in sequence
   * @return char based sequence
   */
  private static CharSubSequence of(CharSequence charSequence, int startIndex, int endIndex) {
    CharSubSequence charSubSequence;

    if (charSequence instanceof CharSubSequence) {
      charSubSequence = ((CharSubSequence) charSequence);
    } else if (charSequence instanceof String) {
      charSubSequence =
          new CharSubSequence(
              ((String) charSequence).toCharArray(), ((String) charSequence).hashCode());
    } else if (charSequence instanceof StringBuilder) {
      char[] chars = new char[charSequence.length()];
      ((StringBuilder) charSequence).getChars(0, charSequence.length(), chars, 0);
      charSubSequence = new CharSubSequence(chars, 0);
    } else {
      charSubSequence = new CharSubSequence(charSequence.toString().toCharArray(), 0);
    }

    if (startIndex == 0 && endIndex == charSequence.length()) {
      return charSubSequence;
    }

    return charSubSequence.subSequence(startIndex, endIndex);
  }
}
