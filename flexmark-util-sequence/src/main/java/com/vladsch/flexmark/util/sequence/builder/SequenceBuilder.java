package com.vladsch.flexmark.util.sequence.builder;

import com.vladsch.flexmark.util.misc.CharPredicate;
import com.vladsch.flexmark.util.sequence.BasedOptionsHolder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.Range;
import com.vladsch.flexmark.util.sequence.SegmentedSequence;
import java.util.HashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** A Builder for Segmented BasedSequences */
public class SequenceBuilder implements ISequenceBuilder<SequenceBuilder, BasedSequence> {
  private final BasedSegmentBuilder segments;
  private final @NotNull BasedSequence baseSeq;
  private final @NotNull BasedSequence
      altBase; // sequence used for creating the builder, needed for validation for alt sequence
  // creation
  private final @NotNull HashMap<BasedSequence, Boolean> equivalentBases;
  private @Nullable BasedSequence resultSeq;

  /**
   * Construct a base sequence builder for given base sequence with default options.
   *
   * <p>NOTE: the builder is always constructed for the base sequence of the base. ie. for the based
   * sequence returned by {@link BasedSequence#getBaseSequence()}, so any subsequence from a base
   * can be used as argument for the constructor
   *
   * <p>
   *
   * @param base base sequence for which to create a builder
   * @param optimizer optimizer for based segment builder, or default {@link CharRecoveryOptimizer}
   */
  private SequenceBuilder(@NotNull BasedSequence base, @Nullable SegmentOptimizer optimizer) {
    this(base, optimizer, new HashMap<>());
  }

  private SequenceBuilder(
      @NotNull BasedSequence base,
      @Nullable SegmentOptimizer optimizer,
      @NotNull HashMap<BasedSequence, Boolean> equivalentBases) {
    altBase = base;
    baseSeq = base.getBaseSequence();
    this.equivalentBases = equivalentBases;
    int options = ISegmentBuilder.F_DEFAULT;
    // NOTE: if full segmented is not specified, then collect first256 stats for use by tree impl
    if (!baseSeq.anyOptions(BasedOptionsHolder.F_FULL_SEGMENTED_SEQUENCES)
        || baseSeq.anyOptions(BasedOptionsHolder.F_COLLECT_FIRST256_STATS))
      options |= ISegmentBuilder.F_TRACK_FIRST256;
    if (baseSeq.anyOptions(BasedOptionsHolder.F_NO_ANCHORS))
      options &= ~ISegmentBuilder.F_INCLUDE_ANCHORS;
    segments =
        optimizer == null
            ? BasedSegmentBuilder.emptyBuilder(baseSeq, options)
            : BasedSegmentBuilder.emptyBuilder(baseSeq, optimizer, options);
  }

  /**
   * Construct a base sequence builder for given base sequence with specific options.
   *
   * <p>NOTE: the builder is always constructed for the base sequence of the base. ie. for the based
   * sequence returned by {@link BasedSequence#getBaseSequence()}, so any subsequence from a base
   * can be used as argument for the constructor
   *
   * <p>
   *
   * @param base base sequence for which to create a builder
   * @param options builder options
   * @param optimizer optimizer for based segment builder, or default {@link CharRecoveryOptimizer}
   */
  private SequenceBuilder(
      @NotNull BasedSequence base,
      int options,
      @Nullable SegmentOptimizer optimizer,
      @NotNull HashMap<BasedSequence, Boolean> equivalentBases) {
    altBase = base;
    baseSeq = base.getBaseSequence();
    this.equivalentBases = equivalentBases;
    // NOTE: if full segmented is not specified, then collect first256 stats for use by tree impl
    if (!baseSeq.anyOptions(BasedOptionsHolder.F_FULL_SEGMENTED_SEQUENCES)
        || baseSeq.anyOptions(BasedOptionsHolder.F_COLLECT_FIRST256_STATS))
      options |= ISegmentBuilder.F_TRACK_FIRST256;
    if (baseSeq.anyOptions(BasedOptionsHolder.F_NO_ANCHORS))
      options &= ~ISegmentBuilder.F_INCLUDE_ANCHORS;
    segments =
        optimizer == null
            ? BasedSegmentBuilder.emptyBuilder(baseSeq, options)
            : BasedSegmentBuilder.emptyBuilder(baseSeq, optimizer, options);
  }

  @NotNull
  public BasedSequence getBaseSequence() {
    return baseSeq;
  }

  @NotNull
  public BasedSegmentBuilder getSegmentBuilder() {
    return segments;
  }

  @Nullable
  @Override
  public BasedSequence getSingleBasedSequence() {
    Range range = segments.getBaseSubSequenceRange();
    return range == null ? null : baseSeq.subSequence(range.getStart(), range.getEnd());
  }

  @NotNull
  @Override
  public SequenceBuilder getBuilder() {
    return new SequenceBuilder(altBase, segments.options, segments.optimizer, equivalentBases);
  }

  @Override
  public char charAt(int index) {
    return toSequence().charAt(index);
  }

  private boolean isCommonBaseSequence(@NotNull BasedSequence chars) {
    if (chars.isNull()) {
      return false;
    }

    BasedSequence charsBaseSequence = chars.getBaseSequence();
    if (charsBaseSequence == baseSeq) {
      return true;
    }

    // see if it is known to be equivalent or not equivalent
    Boolean inCommon = equivalentBases.get(charsBaseSequence);
    if (inCommon != null) {
      return inCommon;
    }

    boolean equivalent = baseSeq.equals(charsBaseSequence);
    equivalentBases.put(charsBaseSequence, equivalent);
    return equivalent;
  }

  @NotNull
  @Override
  public SequenceBuilder append(@Nullable CharSequence chars, int startIndex, int endIndex) {
    if (chars instanceof BasedSequence && isCommonBaseSequence((BasedSequence) chars)) {
      if (((BasedSequence) chars).isNotNull()) {
        if (startIndex == 0 && endIndex == chars.length()) {
          ((BasedSequence) chars).addSegments(segments);
        } else {
          ((BasedSequence) chars).subSequence(startIndex, endIndex).addSegments(segments);
        }
        resultSeq = null;
      }
    } else if (chars != null && startIndex < endIndex) {
      if (startIndex == 0 && endIndex == chars.length()) {
        segments.append(chars);
      } else {
        segments.append(chars.subSequence(startIndex, endIndex));
      }
      resultSeq = null;
    }
    return this;
  }

  @Override
  @NotNull
  public SequenceBuilder append(char c) {
    segments.append(c);
    resultSeq = null;
    return this;
  }

  @Override
  @NotNull
  public SequenceBuilder append(char c, int count) {
    if (count > 0) {
      segments.append(c, count);
      resultSeq = null;
    }
    return this;
  }

  @NotNull
  public SequenceBuilder append(int startOffset, int endOffset) {
    return addByOffsets(startOffset, endOffset);
  }

  @NotNull
  SequenceBuilder append(@NotNull Range chars) {
    return addRange(chars);
  }

  @NotNull
  private SequenceBuilder addRange(@NotNull Range range) {
    segments.append(range);
    resultSeq = null;
    return this;
  }

  @NotNull
  private SequenceBuilder addByOffsets(int startOffset, int endOffset) {
    if (startOffset < 0 || startOffset > endOffset || endOffset > baseSeq.length()) {
      throw new IllegalArgumentException(
          "addByOffsets start/end must be a valid range in [0, "
              + baseSeq.length()
              + "], got: ["
              + startOffset
              + ", "
              + endOffset
              + "]");
    }
    segments.append(Range.of(startOffset, endOffset));
    resultSeq = null;
    return this;
  }

  @NotNull
  @Override
  public BasedSequence toSequence() {
    if (resultSeq == null) {
      resultSeq = SegmentedSequence.create(this);
    }
    return resultSeq;
  }

  /**
   * Construct sequence from this builder using another based sequence which is character identical
   * to this builder's baseSeq
   *
   * @param altSequence based sequence which is character identical to this builder's baseSeq
   * @return builder with offsets mapped to altSequence
   */
  @NotNull
  public BasedSequence toSequence(@NotNull BasedSequence altSequence) {
    return toSequence(altSequence, null, null);
  }

  /**
   * Construct sequence from this builder using another based sequence which is character identical
   * to this builder's baseSeq
   *
   * @param altSequence based sequence which is character identical to this builder's baseSeq
   * @param trimStart character set of characters to trim
   * @param ignoreCharDiff chars which should be treated as equivalent for verification purposes
   *     (Space, Tab, EOL, usually)
   * @return builder with offsets mapped to altSequence
   */
  @NotNull
  public BasedSequence toSequence(
      @NotNull BasedSequence altSequence,
      @Nullable CharPredicate trimStart,
      @Nullable CharPredicate ignoreCharDiff) {
    if (altSequence == altBase) {
      return toSequence();
    }

    // this is an identical but different base sequence, need to map to it. Ranges are indices into
    // altSequence and must be converted to offsets.
    SequenceBuilder altBuilder =
        new SequenceBuilder(altSequence, segments.options, segments.optimizer, new HashMap<>());

    int deleted = 0;
    for (Object part : segments) {
      if (part instanceof Range) {
        BasedSequence s =
            altSequence.subSequence(
                deleted + ((Range) part).getStart(), deleted + ((Range) part).getEnd());
        int startTrimmed = trimStart == null ? 0 : s.countLeading(trimStart);

        if (startTrimmed > 0) {
          deleted += startTrimmed;
          s =
              altSequence.subSequence(
                  deleted + ((Range) part).getStart(), deleted + ((Range) part).getEnd());
          // NOTE: here there could be differences in space vs tab vs EOL due to shift and wrapping
        }
        altBuilder.append(s);
      } else if (part instanceof CharSequence) {
        altBuilder.append((CharSequence) part);
      } else if (part != null) {
        throw new IllegalStateException("Invalid part type " + part.getClass());
      }
    }

    BasedSequence result = SegmentedSequence.create(altBuilder);
    return result;
  }

  @Override
  public int length() {
    return segments.length();
  }

  @NotNull
  public String toStringWithRanges() {
    return segments.toStringWithRangesVisibleWhitespace(baseSeq);
  }

  @NotNull
  public String toStringWithRanges(boolean toVisibleWhiteSpace) {
    return toVisibleWhiteSpace
        ? segments.toStringWithRangesVisibleWhitespace(baseSeq)
        : segments.toStringWithRanges(baseSeq);
  }

  @Override
  @NotNull
  public String toString() {
    StringBuilder sb = new StringBuilder();
    for (Object part : segments) {
      if (part instanceof Range) {
        BasedSequence s = baseSeq.subSequence(((Range) part).getStart(), ((Range) part).getEnd());

        if (s.isNotEmpty()) {
          s.appendTo(sb);
        }
      } else if (part instanceof CharSequence) {
        sb.append(part);
      } else if (part != null) {
        throw new IllegalStateException("Invalid part type " + part.getClass());
      }
    }
    return sb.toString();
  }

  @NotNull
  public static SequenceBuilder emptyBuilder(@NotNull BasedSequence base) {
    return new SequenceBuilder(base, null);
  }

  @NotNull
  static SequenceBuilder emptyBuilder(
      @NotNull BasedSequence base, @NotNull SegmentOptimizer optimizer) {
    return new SequenceBuilder(base, optimizer);
  }

  @NotNull
  public static SequenceBuilder emptyBuilder(@NotNull BasedSequence base, int options) {
    return new SequenceBuilder(base, options, null, new HashMap<>());
  }

  @NotNull
  static SequenceBuilder emptyBuilder(
      @NotNull BasedSequence base, int options, @NotNull SegmentOptimizer optimizer) {
    return new SequenceBuilder(base, options, optimizer, new HashMap<>());
  }
}
