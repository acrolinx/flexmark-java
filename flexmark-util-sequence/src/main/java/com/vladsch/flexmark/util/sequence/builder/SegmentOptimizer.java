package com.vladsch.flexmark.util.sequence.builder;

import java.util.function.BiFunction;

interface SegmentOptimizer extends BiFunction<CharSequence, Object[], Object[]> {
  /**
   * Optimize segment BASE parts surrounding TEXT contained in Object[] array.
   *
   * @param chars base character sequence
   * @param objects parts to optimize Object[0] - previous BASE Range, will be Range.NULL if no
   *     previous range Object[1] - char sequence of TEXT to optimize Object[2] - next BASE Range,
   *     will be Range.NULL if no next range
   * @return Object[] containing optimized segments, non-null Range(s) are BASE segments,
   *     CharSequence(s) are TEXT segments null entry ignored, an optimal filler for unused entries
   *     Range with -ve start/end or -ve span are skipped CharSequence with 0 length skipped
   */
  @Override
  Object[] apply(CharSequence chars, Object[] objects);
}
