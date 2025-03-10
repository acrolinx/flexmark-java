package com.vladsch.flexmark.util.sequence;

import com.vladsch.flexmark.util.sequence.mappers.CharMapper;

class NullEncoder {
  static final CharMapper encodeNull = new EncodeNull();
  static final CharMapper decodeNull = new DecodeNull();

  private static class DecodeNull implements CharMapper {
    DecodeNull() {}

    @Override
    public char map(char c) {
      return c == SequenceUtils.ENC_NUL ? SequenceUtils.NUL : c;
    }
  }

  private static class EncodeNull implements CharMapper {
    EncodeNull() {}

    @Override
    public char map(char c) {
      return c == SequenceUtils.NUL ? SequenceUtils.ENC_NUL : c;
    }
  }
}
