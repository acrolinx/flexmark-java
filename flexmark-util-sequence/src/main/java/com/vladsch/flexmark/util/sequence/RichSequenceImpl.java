package com.vladsch.flexmark.util.sequence;

import com.vladsch.flexmark.util.sequence.builder.ISequenceBuilder;
import com.vladsch.flexmark.util.sequence.builder.RichSequenceBuilder;
import com.vladsch.flexmark.util.sequence.mappers.CharMapper;

/** A RichSequence implementation */
class RichSequenceImpl extends IRichSequenceBase<RichSequence> implements RichSequence {
  private final CharSequence charSequence;

  private RichSequenceImpl(CharSequence charSequence) {
    super(charSequence instanceof String ? charSequence.hashCode() : 0);
    this.charSequence = charSequence;
  }

  @Override
  public RichSequence[] emptyArray() {
    return EMPTY_ARRAY;
  }

  @Override
  public RichSequence nullSequence() {
    return NULL;
  }

  @Override
  public RichSequence sequenceOf(CharSequence charSequence, int startIndex, int endIndex) {
    return RichSequence.of(charSequence, startIndex, endIndex);
  }

  @Override
  public <B extends ISequenceBuilder<B, RichSequence>> B getBuilder() {
    return (B) RichSequenceBuilder.emptyBuilder();
  }

  @Override
  public RichSequence subSequence(int startIndex, int endIndex) {
    SequenceUtils.validateStartEnd(startIndex, endIndex, length());
    if (startIndex == 0 && endIndex == charSequence.length()) {
      return this;
    }
    return create(charSequence, startIndex, endIndex);
  }

  @Override
  public int length() {
    return charSequence.length();
  }

  @Override
  public char charAt(int index) {
    char c = charSequence.charAt(index);
    return c == SequenceUtils.NUL ? SequenceUtils.ENC_NUL : c;
  }

  @Override
  public RichSequence toMapped(CharMapper mapper) {
    return MappedRichSequence.mappedOf(mapper, this);
  }

  static RichSequence create(CharSequence charSequence, int startIndex, int endIndex) {
    if (charSequence instanceof RichSequence) {
      return ((RichSequence) charSequence).subSequence(startIndex, endIndex);
    } else if (charSequence != null) {
      if (startIndex == 0 && endIndex == charSequence.length()) {
        return new RichSequenceImpl(charSequence);
      }
      return new RichSequenceImpl(charSequence.subSequence(startIndex, endIndex));
    } else {
      return NULL;
    }
  }
}
