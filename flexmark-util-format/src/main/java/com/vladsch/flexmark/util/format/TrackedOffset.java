package com.vladsch.flexmark.util.format;

import com.vladsch.flexmark.util.misc.BitFieldSet;

/**
 * Tracked Offset information
 *
 * <p>NOTE: purposefully equals compares the offset only and will equal an integer of the same value
 * to allow use of TrackedOffset as a key but lookup to be done by offset
 */
public final class TrackedOffset implements Comparable<TrackedOffset> {
  private enum Flags {
    AFTER_SPACE_EDIT,
    AFTER_INSERT,
    AFTER_DELETE,
  }

  private static final int F_AFTER_SPACE_EDIT = BitFieldSet.intMask(Flags.AFTER_SPACE_EDIT);
  private static final int F_AFTER_INSERT = BitFieldSet.intMask(Flags.AFTER_INSERT);
  private static final int F_AFTER_DELETE = BitFieldSet.intMask(Flags.AFTER_DELETE);

  private final TrackedOffset original;
  private final int offset;
  private final int flags;
  private int spacesBefore;
  private int spacesAfter;
  private boolean isSpliced; // spaces reset to 0
  private int index;

  private TrackedOffset(
      int offset, boolean afterSpaceEdit, boolean afterInsert, boolean afterDelete) {
    this.original = null;
    this.offset = offset;
    int flags = 0;
    if (afterSpaceEdit) flags |= F_AFTER_SPACE_EDIT;
    if (afterInsert) flags |= F_AFTER_INSERT;
    if (afterDelete) flags |= F_AFTER_DELETE;
    this.flags = flags;
    this.index = -1;
    this.spacesBefore = -1;
    this.spacesAfter = -1;
  }

  private TrackedOffset(TrackedOffset other) {
    this.original = other.original;
    this.offset = other.offset;
    this.flags = other.flags;
    this.index = -1;
    this.spacesBefore = other.spacesBefore;
    this.spacesAfter = other.spacesAfter;
  }

  private TrackedOffset(TrackedOffset other, int offset) {
    this.original = other;
    this.offset = offset;
    this.flags = other.flags;
    this.index = -1;
    this.spacesBefore = other.spacesBefore;
    this.spacesAfter = other.spacesAfter;
  }

  public int getOffset() {
    return offset;
  }

  public int getSpacesBefore() {
    return spacesBefore;
  }

  public void setSpacesBefore(int spacesBefore) {
    this.spacesBefore = spacesBefore;
  }

  public int getSpacesAfter() {
    return spacesAfter;
  }

  public void setSpacesAfter(int spacesAfter) {
    this.spacesAfter = spacesAfter;
  }

  public boolean isSpliced() {
    return isSpliced;
  }

  public void setSpliced(boolean spliced) {
    this.isSpliced = spliced;
  }

  public boolean isResolved() {
    return index != -1;
  }

  public int getIndex() {
    return index == -1 ? offset : index;
  }

  public void setIndex(int index) {
    if (this.original != null) this.original.index = index;
    this.index = index;
  }

  public boolean isAfterSpaceEdit() {
    return BitFieldSet.any(flags, F_AFTER_SPACE_EDIT);
  }

  public boolean isAfterInsert() {
    return BitFieldSet.any(flags, F_AFTER_INSERT);
  }

  public boolean isAfterDelete() {
    return BitFieldSet.any(flags, F_AFTER_DELETE);
  }

  @Override
  public int compareTo(TrackedOffset o) {
    return Integer.compare(offset, o.offset);
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || (getClass() != object.getClass() && !(object instanceof Integer))) {
      return false;
    }

    if (object instanceof Integer) {
      return ((Integer) object) == offset;
    }

    TrackedOffset offset = (TrackedOffset) object;
    return this.offset == offset.offset;
  }

  @Override
  public int hashCode() {
    return offset;
  }

  @Override
  public String toString() {
    return "{"
        + offset
        + (isSpliced() ? " ><" : "")
        + (spacesBefore >= 0 || spacesAfter >= 0
            ? " "
                + (spacesBefore >= 0 ? Integer.toString(spacesBefore) : "?")
                + "|"
                + (spacesAfter >= 0 ? Integer.toString(spacesAfter) : "?")
            : "")
        + (BitFieldSet.any(flags, F_AFTER_SPACE_EDIT | F_AFTER_INSERT | F_AFTER_DELETE)
            ? " "
                + (isAfterSpaceEdit() ? "s" : "")
                + (isAfterInsert() ? "i" : "")
                + (isAfterDelete() ? "d" : "")
            : "")
        + (isResolved() ? " -> " + index : "")
        + "}";
  }

  static TrackedOffset track(int offset) {
    return track(offset, false, false, false);
  }

  public static TrackedOffset track(int offset, Character c, boolean afterDelete) {
    return track(offset, c != null && c == ' ', c != null && !afterDelete, afterDelete);
  }

  public static TrackedOffset track(
      int offset, boolean afterSpaceEdit, boolean afterInsert, boolean afterDelete) {
    return new TrackedOffset(offset, afterSpaceEdit, afterInsert, afterDelete);
  }
}
