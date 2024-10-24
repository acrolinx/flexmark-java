package com.vladsch.flexmark.util.format;

import com.vladsch.flexmark.util.misc.CharPredicate;

public interface CharWidthProvider {
  int getSpaceWidth();

  int getCharWidth(char c);

  default int getStringWidth(CharSequence chars) {
    return getStringWidth(chars, CharPredicate.NONE);
  }

  default int getStringWidth(CharSequence chars, CharPredicate zeroWidthChars) {
    int iMax = chars.length();
    int width = 0;

    for (int i = 0; i < iMax; i++) {
      char c = chars.charAt(i);
      if (!zeroWidthChars.test(c)) {
        width += getCharWidth(c);
      }
    }
    return width;
  }

  CharWidthProvider NULL =
      new CharWidthProvider() {
        @Override
        public int getSpaceWidth() {
          return 1;
        }

        @Override
        public int getCharWidth(char c) {
          return 1;
        }
      };
}
