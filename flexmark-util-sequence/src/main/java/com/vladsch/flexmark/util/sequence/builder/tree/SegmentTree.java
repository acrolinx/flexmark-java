package com.vladsch.flexmark.util.sequence.builder.tree;

import static com.vladsch.flexmark.util.sequence.builder.tree.Segment.SegType.ANCHOR;
import static com.vladsch.flexmark.util.sequence.builder.tree.Segment.SegType.BASE;

import com.vladsch.flexmark.util.misc.DelimitedBuilder;
import com.vladsch.flexmark.util.sequence.BasedSequence;
import com.vladsch.flexmark.util.sequence.builder.IBasedSegmentBuilder;
import com.vladsch.flexmark.util.sequence.builder.Seg;

/** Binary search tree of sequence segments */
public class SegmentTree {
  public static final int MAX_VALUE = Integer.MAX_VALUE >> 2;
  public static final int F_ANCHOR_FLAGS = ~MAX_VALUE;

  private final int[]
      treeData; // tuples of aggregated length, segment byte offset with flags for prev anchor
  // offset of 1 to 7
  protected final byte[] segmentBytes; // bytes of serialized segments

  protected SegmentTree(int[] treeData, byte[] segmentBytes) {
    this.treeData = treeData;
    this.segmentBytes = segmentBytes;
  }

  public int[] getTreeData() {
    return treeData;
  }

  public byte[] getSegmentBytes() {
    return segmentBytes;
  }

  public int size() {
    return treeData.length / 2;
  }

  public int aggrLength(int pos) {
    return pos < 0 ? 0 : treeData[pos << 1];
  }

  private int byteOffsetData(int pos) {
    return treeData[(pos << 1) + 1];
  }

  int byteOffset(int pos) {
    return getByteOffset(treeData[(pos << 1) + 1]);
  }

  private static int getByteOffset(int byteOffsetData) {
    int offset = byteOffsetData & MAX_VALUE;
    return offset == MAX_VALUE ? -1 : offset;
  }

  static int getAnchorOffset(int byteOffsetData) {
    return (byteOffsetData & F_ANCHOR_FLAGS) >>> 29;
  }

  public boolean hasPreviousAnchor(int pos) {
    return getAnchorOffset(treeData[(pos << 1) + 1]) > 0;
  }

  public int previousAnchorOffset(int pos) {
    int byteOffsetData = byteOffsetData(pos);
    return getByteOffset(byteOffsetData) - getAnchorOffset(byteOffsetData);
  }

  Segment getSegment(int byteOffset, int pos, int startIndex, BasedSequence baseSeq) {
    return Segment.getSegment(segmentBytes, byteOffset, pos, startIndex, baseSeq);
  }

  public Segment findSegment(int index, BasedSequence baseSeq, Segment hint) {
    return findSegment(index, 0, size(), baseSeq, hint);
  }

  public Segment findSegment(
      int index, int startPos, int endPos, BasedSequence baseSeq, Segment hint) {
    if (hint != null) {
      // NOTE: first try around cached segment for this index
      int startIndex = hint.getStartIndex();
      if (index >= startIndex) {
        int endIndex = hint.getEndIndex();
        if (hint.pos + 1 >= endPos) {
          return null;
        }
        int nextLength = aggrLength(hint.pos + 1);
        if (index < nextLength) {
          // FIX: add stats to track this
          return Segment.getSegment(
              segmentBytes, byteOffset(hint.pos + 1), hint.pos + 1, endIndex, baseSeq);
        }
        // can skip next one too
        startPos = hint.pos + 2;
      } else {
        // see if previous contains index
        if (hint.pos == startPos) {
          return null;
        }

        int prevPrevLength = aggrLength(hint.pos - 2);
        if (index >= prevPrevLength) {
          // it is previous one
          // FIX: add stats to track this
          return Segment.getSegment(
              segmentBytes, byteOffset(hint.pos - 1), hint.pos - 1, prevPrevLength, baseSeq);
        }
        // previous one can be skipped
        endPos = hint.pos - 1;
      }
    }

    // NOTE: most of the time char sequence access starts at 0, so we try the start pos
    if (startPos >= 0 && startPos < size()) {
      int firstLength = aggrLength(startPos);
      if (index < firstLength) {
        int prevLength = aggrLength(startPos - 1);
        if (index >= prevLength) {
          // FIX: add stats to track this
          return Segment.getSegment(
              segmentBytes, byteOffset(startPos), startPos, prevLength, baseSeq);
        }
        // first one is too far, we can skip it
        endPos = startPos;
      } else {
        // first one can be skipped
        startPos = startPos + 1;
      }
    }

    // NOTE: failing that we try the last segment in case it is backwards scan through sequence
    if (endPos - 1 >= startPos) {
      // check last one for match
      int secondToLastLength = aggrLength(endPos - 2);
      if (index >= secondToLastLength) {
        int lastLength = aggrLength(endPos - 1);
        if (index >= lastLength) {
          return null; /* beyond last segment*/
        }

        // FIX: add stats to track this
        return Segment.getSegment(
            segmentBytes, byteOffset(endPos - 1), endPos - 1, secondToLastLength, baseSeq);
      }

      // previous to last can be skipped
      endPos = endPos - 1;
    }

    // NOTE: all optimizations failed, but not completely wasted since they served to shorten the
    // search range.
    SegmentTreePos treePos = findSegmentPos(index, startPos, endPos);
    if (treePos != null) {
      return Segment.getSegment(
          segmentBytes, byteOffset(treePos.pos), treePos.pos, treePos.startIndex, baseSeq);
    }
    return null;
  }

  public SegmentTreeRange getSegmentRange(
      int startIndex,
      int endIndex,
      int startPos,
      int endPos,
      BasedSequence baseSequence,
      Segment hint) {
    Segment startSegment;
    Segment endSegment;

    if (startIndex == endIndex) {
      // this is could be an empty suffix so it may be the end of a segment, search for startIndex-1
      // and use that segment as its location
      startSegment =
          hint == null || hint.notInSegment(startIndex)
              ? findSegment(startIndex, startPos, endPos, baseSequence, hint)
              : hint;
      if (startSegment == null) {
        startSegment =
            hint == null || hint.notInSegment(startIndex - 1)
                ? findSegment(startIndex - 1, startPos, endPos, baseSequence, hint)
                : hint;

        // if index is out of the found segment and there is a next segment which contains start
        // index, then use that one
        if (startSegment.notInSegment(startIndex) && startSegment.pos + 1 < size()) {
          Segment nextSegment = getSegment(startSegment.pos + 1, baseSequence);
          if (!nextSegment.notInSegment(startIndex)) {
            startSegment = nextSegment;
          }
        }
      }

      endSegment = startSegment;
    } else {
      startSegment =
          hint == null || hint.notInSegment(startIndex)
              ? findSegment(startIndex, startPos, endPos, baseSequence, hint)
              : hint;
      endSegment =
          !startSegment.notInSegment(endIndex - 1)
              ? startSegment
              : (hint == null || hint.notInSegment(endIndex - 1)
                  ? findSegment(endIndex - 1, startPos, endPos, baseSequence, startSegment)
                  : hint);
    }

    int startOffset = -1;
    int endOffset = -1;

    // if start segment is text then we look for previous anchor or range to get startOffset base
    // context information, failing that look for next range or anchor
    if (startSegment.isText()) {
      startOffset = getTextStartOffset(startSegment, baseSequence);
    } else {
      startOffset = startSegment.getStartOffset() + startIndex - startSegment.getStartIndex();
    }

    // if end segment is text then we look for next anchor or range to get endOffset base context
    // information
    if (endSegment.isText()) {
      endOffset = getTextEndOffset(endSegment, baseSequence);
    } else {
      endOffset = endSegment.getStartOffset() + endIndex - endSegment.getStartIndex();
    }

    if (startOffset < 0) {
      if (startSegment.pos + 1 < size()) {
        Segment nextSegment = getSegment(startSegment.pos + 1, baseSequence);
        startOffset = nextSegment.getStartOffset();
        if (startOffset > endOffset && endOffset != -1) startOffset = endOffset;
      } else {
        startOffset = endOffset;
      }
    }

    if (endOffset < startOffset) endOffset = startOffset;

    if (startOffset > baseSequence.length()) {
      throw new IllegalStateException(
          String.format("startOffset:%d > baseSeq.length: %d", startOffset, baseSequence.length()));
    }

    if (endOffset > baseSequence.length()) {
      throw new IllegalStateException(
          String.format("endOffset:%d > baseSeq.length: %d", endOffset, baseSequence.length()));
    }

    return new SegmentTreeRange(
        startIndex, endIndex, startOffset, endOffset, startSegment.pos, endSegment.pos + 1);
  }

  private int getTextEndOffset(Segment segment, BasedSequence baseSequence) {
    if (segment.pos + 1 < size()) {
      Segment nextSegment = getSegment(segment.pos + 1, baseSequence);
      if (nextSegment.isBase()) {
        return nextSegment.getStartOffset();
      }
    }
    return -1;
  }

  private int getTextStartOffset(Segment segment, BasedSequence baseSequence) {
    Segment prevSegment = getPrevAnchor(segment.pos, baseSequence);
    if (prevSegment == null && segment.pos > 0) {
      prevSegment = getSegment(segment.pos - 1, baseSequence);
    }

    if (prevSegment != null && prevSegment.isBase()) {
      return prevSegment.getEndOffset();
    }
    return -1;
  }

  /**
   * Add segments selected by given treeRange
   *
   * @param builder based segment builder
   * @param treeRange treeRange for which to add segments
   */
  public void addSegments(IBasedSegmentBuilder<?> builder, SegmentTreeRange treeRange) {
    addSegments(
        builder,
        treeRange.startIndex,
        treeRange.endIndex,
        treeRange.startOffset,
        treeRange.endOffset,
        treeRange.startPos,
        treeRange.endPos);
  }

  /**
   * Add segments of subsequence of this tree to builder
   *
   * @param builder builder to which to add the segments
   * @param startIndex start index of sub-sequence of segment tree
   * @param endIndex end index of sub-sequence of segment tree
   * @param startOffset start offset of the subsequence to use as start anchor
   * @param endOffset end offset of the subsequence to use as end anchor
   * @param startPos start pos of sub-sequence segments in tree
   * @param endPos end pos of sub-sequence segments in tree
   */
  public void addSegments(
      IBasedSegmentBuilder<?> builder,
      int startIndex,
      int endIndex,
      int startOffset,
      int endOffset,
      int startPos,
      int endPos) {
    // add our stuff to builder
    if (startOffset != -1) {
      builder.appendAnchor(startOffset);
    }

    int currentEnd = startOffset;
    BasedSequence baseSequence = builder.getBaseSequence();

    for (int i = startPos; i < endPos; i++) {
      Segment segment = getSegment(i, baseSequence);

      if (segment.isText()) {
        // check for previous anchor
        Segment prevAnchor = getPrevAnchor(i, baseSequence);
        if (prevAnchor != null) builder.appendAnchor(prevAnchor.getStartOffset());
      }

      // OPTIMIZE: add append Segment method with start/end offsets to allow builder to extract
      // repeat and first256 information
      //  without needing to scan text, range information does not have any benefit from this
      CharSequence charSequence = getCharSequence(segment, startIndex, endIndex, startPos, endPos);

      if (segment.isText()) {
        builder.append(charSequence);
        // check for next anchor
        int byteOffset = segment.byteOffset + segment.getByteLength();
        if (byteOffset < segmentBytes.length
            && (i + 1 >= size() || byteOffset != byteOffset(i + 1))) {
          Segment nextAnchor = Segment.getSegment(segmentBytes, byteOffset, 0, 0, baseSequence);
          if (nextAnchor.isAnchor()) {
            builder.appendAnchor(nextAnchor.getStartOffset());
          }
        }
      } else {
        BasedSequence basedSequence = (BasedSequence) charSequence;
        currentEnd = Math.max(currentEnd, basedSequence.getEndOffset());
        builder.append(basedSequence.getStartOffset(), basedSequence.getEndOffset());
      }
    }

    if (endOffset != -1) {
      builder.appendAnchor(Math.max(currentEnd, endOffset));
    }
  }

  /**
   * Get char sequence of segment corresponding to sub-sequence in segment tree
   *
   * @param segment segment
   * @param startIndex start index of sub-sequence of segment tree
   * @param endIndex end index of sub-sequence of segment tree
   * @param startPos start pos of sub-sequence segments in tree
   * @param endPos end pos of sub-sequence segments in tree
   * @return subsequence of segment corresponding to part of it which is in the sub-sequence of the
   *     tree
   */
  private static CharSequence getCharSequence(
      Segment segment, int startIndex, int endIndex, int startPos, int endPos) {
    CharSequence charSequence;
    int pos = segment.pos;

    if (pos == startPos && pos + 1 == endPos) {
      // need to trim start/end
      charSequence =
          segment
              .getCharSequence()
              .subSequence(
                  startIndex - segment.getStartIndex(), endIndex - segment.getStartIndex());
    } else if (pos == startPos) {
      // need to trim start
      charSequence =
          segment
              .getCharSequence()
              .subSequence(startIndex - segment.getStartIndex(), segment.length());
    } else if (pos + 1 == endPos) {
      // need to trim end
      charSequence = segment.getCharSequence().subSequence(0, endIndex - segment.getStartIndex());
    } else {
      charSequence = segment.getCharSequence();
    }

    return charSequence;
  }

  public SegmentTreePos findSegmentPos(int index, int startPos, int endPos) {
    return findSegmentPos(index, treeData, startPos, endPos);
  }

  public Segment getSegment(int pos, BasedSequence baseSeq) {
    return Segment.getSegment(segmentBytes, byteOffset(pos), pos, aggrLength(pos - 1), baseSeq);
  }

  public Segment getPrevAnchor(int pos, BasedSequence baseSeq) {
    return getPrevAnchor(pos, treeData, segmentBytes, baseSeq);
  }

  public String toString(BasedSequence baseSeq) {
    DelimitedBuilder out = new DelimitedBuilder(", ");
    out.append(getClass().getSimpleName()).append("{aggr: {");
    int iMax = size();
    for (int i = 0; i < iMax; i++) {
      out.append("[").append(aggrLength(i)).append(", ").append(byteOffset(i)).append(":");
      if (hasPreviousAnchor(i)) {
        out.append(", ").append(previousAnchorOffset(i)).append(":");
      }
      out.append("]").mark();
    }

    out.unmark().append(" }, seg: { ");
    int offset = 0;
    while (offset < segmentBytes.length) {
      Segment segment = Segment.getSegment(segmentBytes, offset, 0, 0, baseSeq);
      out.append(offset).append(":").append(segment).mark();
      offset += segment.getByteLength();
    }
    out.unmark().append(" } }");
    return out.toString();
  }

  @Override
  public String toString() {
    return toString(BasedSequence.NULL);
  }

  // Implementation is static to allow not having to use the class but just its computed data
  public static int aggrLength(int pos, int[] treeData) {
    return pos < 0 ? 0 : treeData[pos << 1];
  }

  private static int byteOffsetData(int pos, int[] treeData) {
    return treeData[(pos << 1) + 1];
  }

  public static int byteOffset(int pos, int[] treeData) {
    return getByteOffset(byteOffsetData(pos, treeData));
  }

  private static void setTreeData(
      int pos, int[] treeData, int agrrLength, int byteOffset, int prevAnchorOffset) {
    treeData[pos << 1] = agrrLength;
    treeData[(pos << 1) + 1] = byteOffset | (prevAnchorOffset == 0 ? 0 : prevAnchorOffset << 29);
  }

  public static boolean hasPreviousAnchor(int pos, int[] treeData) {
    return getAnchorOffset(treeData[(pos << 1) + 1]) > 0;
  }

  public static int previousAnchorOffset(int pos, int[] treeData) {
    int byteOffsetData = byteOffsetData(pos, treeData);
    return getByteOffset(byteOffsetData) - getAnchorOffset(byteOffsetData);
  }

  public static SegmentTreePos findSegmentPos(int index, int[] treeData, int startPos, int endPos) {
    // FIX: add segmented sequence stats collection for iteration counts
    // FIX: check first segment and last segment in case it is a scan from start/end of sequence
    if (index == 0 && startPos == 0) {
      return new SegmentTreePos(0, 0, 0);
    }

    int iterations = 0;
    while (startPos < endPos) {
      int pos = (startPos + endPos) >> 1;

      iterations++;

      int endIndex = aggrLength(pos, treeData);
      if (index >= endIndex) {
        startPos = pos + 1;
      } else {
        int startIndex = aggrLength(pos - 1, treeData);
        if (index < startIndex) {
          endPos = pos;
        } else {
          return new SegmentTreePos(pos, startIndex, iterations);
        }
      }
    }
    return null;
  }

  private static Segment getPrevAnchor(
      int pos, int[] treeData, byte[] segmentBytes, BasedSequence baseSeq) {
    int byteOffsetData = byteOffsetData(pos, treeData);
    int anchorOffset = getAnchorOffset(byteOffsetData);
    if (anchorOffset > 0) {
      int byteOffset = getByteOffset(byteOffsetData) - anchorOffset;
      return Segment.getSegment(segmentBytes, byteOffset, -1, 0, baseSeq);
    }
    return null;
  }

  protected static class SegmentTreeData {
    final int[]
        treeData; // tuples of aggregated length, segment byte offset with flags for prev anchor
    // offset of 1 to 7
    final byte[] segmentBytes; // bytes of serialized segments
    final int[] startIndices; // start index for each segment within the string

    private SegmentTreeData(int[] treeData, byte[] segmentBytes, int[] startIndices) {
      this.treeData = treeData;
      this.segmentBytes = segmentBytes;
      this.startIndices = startIndices;
    }
  }

  public static SegmentTree build(Iterable<Seg> segments, CharSequence allText) {
    SegmentTreeData segmentTreeData = buildTreeData(segments, allText, true);
    return new SegmentTree(segmentTreeData.treeData, segmentTreeData.segmentBytes);
  }

  /**
   * Build binary tree search data
   *
   * <p>Index data has aggregated lengths with BASE and TEXT segments in the data, Offset data has
   * segment start offset with BASE and ANCHOR segments in the data since TEXT segments have no
   * offset they are skipped
   *
   * <p>The offset data can be used to pass as treeData to {@link #findSegmentPos(int, int[], int,
   * int)} with desired offset instead of index to find a segment which can contain the desired
   * offset, with some post processing logic to handle offset segments which are not in the data
   *
   * @param segments segments of the tree
   * @param allText all out of base text
   * @param buildIndexData true to build index search data, false to build base offset tree data
   * @return segment tree instance with the data
   */
  static SegmentTreeData buildTreeData(
      Iterable<Seg> segments, CharSequence allText, boolean buildIndexData) {
    int byteLength = 0;
    int nonAnchors = 0;
    int lastEndOffset = 0;

    for (Seg seg : segments) {
      Segment.SegType segType = Segment.getSegType(seg, allText);
      byteLength += Segment.getSegByteLength(segType, seg.getSegStart(), seg.length());
      if (buildIndexData ? segType != ANCHOR : segType == BASE || segType == ANCHOR) nonAnchors++;
      lastEndOffset = seg.getEnd();
    }

    int[] treeData = new int[nonAnchors * 2];
    byte[] segmentBytes = new byte[byteLength];
    int[] startIndices = buildIndexData ? null : new int[nonAnchors];
    int[] posNeedingAdjustment =
        buildIndexData
            ? null
            : new int
                [2]; // up to 2 segment adjustments, one for BASE sequence and one for TEXT since it
    // has no offsets
    int posNeedingAdjustmentIndex = 0;

    int prevAnchorOffset = -1;

    int pos = 0;
    int offset = 0;
    int aggrLength = 0;
    int segOffset = 0;

    for (Seg seg : segments) {
      segOffset = offset;

      offset = Segment.addSegBytes(segmentBytes, offset, seg, allText);
      Segment.SegType segType = Segment.SegType.fromTypeMask(segmentBytes[segOffset]);

      if (buildIndexData) {
        if (segType == ANCHOR) {
          prevAnchorOffset = segOffset;
        } else {
          aggrLength += seg.length();
          setTreeData(
              pos,
              treeData,
              aggrLength,
              segOffset,
              prevAnchorOffset == -1 ? 0 : segOffset - prevAnchorOffset);
          pos++;
          prevAnchorOffset = -1;
        }
      } else {
        startIndices[pos] = aggrLength;

        if (posNeedingAdjustmentIndex > 0 && seg.getStart() >= 0) {
          // set it to the correct value
          int iMax = posNeedingAdjustmentIndex;
          for (int i = 0; i < iMax; i++) {
            treeData[posNeedingAdjustment[i] << 1] = seg.getStart();
          }
          posNeedingAdjustmentIndex = 0;
        }

        aggrLength += seg.length();

        if (segType == BASE || segType == ANCHOR) {
          // the use of getEnd() here is temporary for all but the last base segment, it will be
          // overwritten by getStart() by next segment
          setTreeData(pos, treeData, seg.getEnd(), segOffset, 0);
          posNeedingAdjustment[posNeedingAdjustmentIndex++] = pos;
          pos++;
        }
      }
    }

    // NOTE: need to fix-up start/end offsets of the tree data since text has no start/end except as
    // previous node end and next node start correspondingly
    if (!buildIndexData) {
      for (int i = 0; i < posNeedingAdjustmentIndex; i++) {
        treeData[posNeedingAdjustment[i] << 1] = lastEndOffset;
      }
    }

    return new SegmentTreeData(treeData, segmentBytes, startIndices);
  }

  /**
   * Build an offset segment tree from this index segment tree
   *
   * <p>Efficiently reuses segmentBytes and only computes offset treeData for BASE and ANCHOR
   * segments
   *
   * @param baseSeq base sequence for the sequence for this segment tree
   * @return SegmentOffsetTree for this segment tree
   */
  SegmentOffsetTree getSegmentOffsetTree(BasedSequence baseSeq) {
    int nonAnchors = 0;
    int byteLength = segmentBytes.length;
    int segOffset = 0;
    int lastEndOffset = 0;

    while (segOffset < byteLength) {
      Segment seg = Segment.getSegment(segmentBytes, segOffset, nonAnchors, 0, baseSeq);
      segOffset += seg.getByteLength();
      if (seg.isBase()) {
        nonAnchors++;
        lastEndOffset = seg.getEndOffset();
      }
    }

    int[] treeData = new int[nonAnchors * 2];
    int[] startIndices = new int[nonAnchors];

    int pos = 0;
    segOffset = 0;
    int length = 0;
    int[] posNeedingAdjustment =
        new int
            [2]; // up to 2 segment adjustments, one for BASE sequence and one for TEXT since it has
    // no offsets
    int posNeedingAdjustmentIndex = 0;

    while (segOffset < byteLength) {
      Segment seg = Segment.getSegment(segmentBytes, segOffset, nonAnchors, length, baseSeq);

      if (posNeedingAdjustmentIndex > 0 && seg.getStartOffset() >= 0) {
        // set it to the correct value
        int iMax = posNeedingAdjustmentIndex;
        for (int i = 0; i < iMax; i++) {
          treeData[posNeedingAdjustment[i] << 1] = seg.getStartOffset();
        }
        posNeedingAdjustmentIndex = 0;
      }

      if (seg.isBase()) {
        // the use of getEnd() here is temporary for all but the last base segment, it will be
        // overwritten by getStart() by next segment
        setTreeData(pos, treeData, seg.getEndOffset(), segOffset, 0);
        posNeedingAdjustment[posNeedingAdjustmentIndex++] = pos;
        startIndices[pos] = length;

        pos++;
      }

      segOffset += seg.getByteLength();
      length += seg.length();
    }

    // NOTE: need to fix-up start/end offsets of the tree data since text has no start/end except as
    // previous node end and next node start correspondingly
    for (int i = 0; i < posNeedingAdjustmentIndex; i++) {
      treeData[posNeedingAdjustment[i] << 1] = lastEndOffset;
    }

    return new SegmentOffsetTree(treeData, segmentBytes, startIndices);
  }
}
