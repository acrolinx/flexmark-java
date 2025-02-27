package com.vladsch.flexmark.util.sequence;

import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKeyBase;
import com.vladsch.flexmark.util.data.NullableDataKey;
import com.vladsch.flexmark.util.misc.BitFieldSet;
import com.vladsch.flexmark.util.sequence.builder.SegmentedSequenceStats;

/**
 * Implemented by BasedOptionsSequence, use instance of it to pass to {@link
 * BasedSequence#of(CharSequence)} and options enabled in it will be accessible to all based
 * sequences or uses of these for testing for options or getting options.
 *
 * <p>Only works with SubSequence base not CharArraySequence
 */
public interface BasedOptionsHolder {
  BasedOptionsHolderOptions O_COLLECT_SEGMENTED_STATS =
      BasedOptionsHolderOptions.COLLECT_SEGMENTED_STATS;
  BasedOptionsHolderOptions O_COLLECT_FIRST256_STATS =
      BasedOptionsHolderOptions.COLLECT_FIRST256_STATS;
  BasedOptionsHolderOptions O_NO_ANCHORS = BasedOptionsHolderOptions.NO_ANCHORS;
  BasedOptionsHolderOptions O_FULL_SEGMENTED_SEQUENCES =
      BasedOptionsHolderOptions.FULL_SEGMENTED_SEQUENCES;
  BasedOptionsHolderOptions O_TREE_SEGMENTED_SEQUENCES =
      BasedOptionsHolderOptions.TREE_SEGMENTED_SEQUENCES;

  // NOTE: if no data holder or one with no SEGMENTED_STATS is passed to BasedOptionsSequence, then
  // F_COLLECT_SEGMENTED_STATS flag will be removed from options
  int F_COLLECT_SEGMENTED_STATS =
      BitFieldSet.intMask(
          O_COLLECT_SEGMENTED_STATS); // set if segmented stats collector key is set to non-null
  // value
  int F_COLLECT_FIRST256_STATS =
      BitFieldSet.intMask(
          O_COLLECT_FIRST256_STATS); // collect statistics for segments sequence on chars < code
  // 256, used to optimize out of base chars for ascii
  int F_NO_ANCHORS =
      BitFieldSet.intMask(
          O_NO_ANCHORS); // do not include anchors in segment builder, test only, not guaranteed to
  // be stable for general use

  // NOTE: if neither is specified then one will be chosen, most likely tree
  //   but may be full for short sequences or ones where number of segments vs
  //   sequence length makes tree based one wasteful and slow
  int F_FULL_SEGMENTED_SEQUENCES =
      BitFieldSet.intMask(O_FULL_SEGMENTED_SEQUENCES); // use full segmented sequences
  int F_TREE_SEGMENTED_SEQUENCES =
      BitFieldSet.intMask(O_TREE_SEGMENTED_SEQUENCES); // use tree based segmented sequences

  // NOTE: if no data holder or one with no SEGMENTED_STATS is passed to BasedOptionsSequence, then
  // F_COLLECT_SEGMENTED_STATS flag will be removed from options
  NullableDataKey<SegmentedSequenceStats> SEGMENTED_STATS =
      new NullableDataKey<>("SEGMENTED_STATS", (SegmentedSequenceStats) null);

  /**
   * Options test for options for this sequence
   *
   * <p>default reports true for global default options (if any) , variation available on
   * BasedSequenceWithOptions
   *
   * @return option flags for this sequence
   */
  int getOptionFlags();

  /**
   * Options test for options for this sequence
   *
   * <p>default reports true for global default options (if any) , variation available on
   * BasedSequenceWithOptions
   *
   * @param options option flags
   * @return true if all option flags passed are set for this sequence
   */
  boolean allOptions(int options);

  /**
   * Options test for options for this sequence
   *
   * <p>default reports true for global default options (if any) , variation available on
   * BasedSequenceWithOptions
   *
   * @param options option flags
   * @return true if any option flags passed are set for this sequence
   */
  boolean anyOptions(int options);

  /**
   * Options holder, default has none, only available on BasedSequenceWithOptions
   *
   * @param <T> type of value held by key
   * @param dataKey in options
   * @return true if data key is available
   */
  <T> T getOption(DataKeyBase<T> dataKey);

  /**
   * Options holder, default has none, only available on BasedSequenceWithOptions
   *
   * @return data holder with options or null if none for this sequence
   */
  DataHolder getOptions();
}
