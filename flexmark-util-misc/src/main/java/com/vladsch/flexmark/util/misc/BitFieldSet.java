package com.vladsch.flexmark.util.misc;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Re-Implementation of RegularEnumSet class for EnumSet, for "regular sized" enum types (i.e.,
 * those with 64 or fewer enum constants)
 *
 * <p>Modification allows access and manipulation of the bit mask for the elements so this class can
 * be easily converted between long/int and BitFieldSet to use as efficient option flags in
 * implementation but convenient enum sets for manipulation.
 *
 * <p>If the Enum implements {@link BitField} then each field can have 1..N bits up to a maximum
 * total of 64 bits per enum. The class provides methods for setting and getting values from these
 * fields as long, int, short or byte values, either signed or unsigned.
 *
 * @author Vladimir Schneider
 * @author Josh Bloch
 * @serial exclude
 * @since 1.5
 */
public class BitFieldSet<E extends Enum<E>> extends AbstractSet<E> implements Serializable {
  private static final long serialVersionUID = 3411599620347842686L;

  private static class UniverseLoader {
    static final ConcurrentHashMap<Class, Enum[]> enumUniverseMap = new ConcurrentHashMap<>();
    static final ConcurrentHashMap<Class, long[]> enumBitMasksMap = new ConcurrentHashMap<>();

    static Enum[] getUniverseSlow(Class elementType) {
      Enum[] cachedUniverse = enumUniverseMap.get(elementType);
      if (cachedUniverse != null) {
        return cachedUniverse;
      }

      Field[] fields = elementType.getFields();
      int enums = 0;
      for (Field field : fields) {
        if (field.getType().isEnum()) enums++;
      }

      if (enums > 0) {
        cachedUniverse = new Enum[enums];

        enums = 0;
        for (Field field : fields) {
          if (field.getType().isEnum()) {
            cachedUniverse[enums++] = Enum.valueOf((Class<Enum>) field.getType(), field.getName());
          }
        }
      } else {
        cachedUniverse = ZERO_LENGTH_ENUM_ARRAY;
      }

      enumUniverseMap.put(elementType, cachedUniverse);
      return cachedUniverse;
    }
  }

  private static long nextBitMask(int nextAvailableBit, int bits) {
    return (-1L >>> -bits) << nextAvailableBit;
  }

  /**
   * Returns all of the values comprising E. The result is cloned and slower than SharedSecrets use
   * but works in Java 11 and Java 8 because SharedSecrets are not shared publicly
   *
   * @param <E> type of enum
   * @param elementType class of enum
   * @return array of enum values
   */
  private static <E extends Enum<E>> E[] getUniverse(Class<E> elementType) {
    return (E[]) UniverseLoader.getUniverseSlow(elementType);
  }

  /**
   * Returns all of the values comprising E. The result is cloned and slower than SharedSecrets use
   * but works in Java 11 and Java 8 because SharedSecrets are not shared publicly
   *
   * @param <E> type of enum
   * @param elementType class of enum
   * @return array of bit masks for enum values
   */
  private static <E extends Enum<E>> long[] getBitMasks(Class<E> elementType) {
    long[] bitMasks = UniverseLoader.enumBitMasksMap.get(elementType);
    if (bitMasks != null) {
      return bitMasks;
    }

    // compute the bit masks for the enum
    E[] universe = (E[]) UniverseLoader.getUniverseSlow(elementType);
    if (BitField.class.isAssignableFrom(elementType)) {
      int bitCount = 0;
      bitMasks = new long[universe.length];

      for (E e : universe) {
        int bits = ((BitField) e).getBits();
        if (bits <= 0)
          throw new IllegalArgumentException(
              String.format(
                  "Enum bit field %s.%s bits must be >= 1, got: %d",
                  elementType.getSimpleName(), e.name(), bits));

        if (bitCount + bits > 64)
          throw new IllegalArgumentException(
              String.format(
                  "Enum bit field %s.%s bits exceed available 64 bits by %d",
                  elementType.getSimpleName(), e.name(), bitCount + bits - 64));

        bitMasks[e.ordinal()] = nextBitMask(bitCount, bits);

        bitCount += bits;
      }
    } else {
      if (universe.length <= 64) {
        bitMasks = new long[universe.length];
        for (E e : universe) {
          bitMasks[e.ordinal()] = 1L << e.ordinal();
        }
      } else {
        throw new IllegalArgumentException("Enums with more than 64 values are not supported");
      }
    }

    UniverseLoader.enumBitMasksMap.put(elementType, bitMasks);
    return bitMasks;
  }

  /**
   * Bit vector representation of this set. The 2^k bit indicates the presence of universe[k] in
   * this set.
   */
  private long elements = 0L;

  /** The class of all the elements of this set. */
  private final Class<E> elementType;

  /** All values comprising T */
  private final E[] universe;

  /** All bit masks for each field since some can span more than one */
  private final long[] bitMasks;

  /** total number of bits used by all fields */
  private final int totalBits; // total bits used by all fields

  private static final Enum<?>[] ZERO_LENGTH_ENUM_ARRAY = new Enum<?>[0];

  private BitFieldSet(Class<E> elementType, Enum<?>[] universe, long[] bitMasks) {
    this.elementType = elementType;
    this.universe = (E[]) universe;
    this.bitMasks = bitMasks;
    this.totalBits = getTotalBits(bitMasks);
  }

  private static int getTotalBits(long[] bitMasks) {
    return bitMasks.length == 0 ? 0 : 64 - Long.numberOfLeadingZeros(bitMasks[bitMasks.length - 1]);
  }

  public int toInt() {
    if (totalBits > 32)
      throw new IllegalArgumentException(
          String.format(
              "Enum fields use %d bits, which is more than 32 bits available in an int",
              totalBits));
    return (int) elements;
  }

  public boolean orMask(long mask) {
    long allValues = -1L >>> -totalBits;
    if ((mask & ~allValues) != 0) {
      throw new IllegalArgumentException(
          String.format(
              "bitMask %d value contains elements outside the universe %s",
              mask, Long.toBinaryString(mask & ~allValues)));
    }

    long oldElements = elements;
    elements |= mask;
    return oldElements != elements;
  }

  public boolean setAll(long mask) {
    long allValues = -1L >>> -totalBits;
    if ((mask & ~allValues) != 0) {
      throw new IllegalArgumentException(
          String.format(
              "mask %d(0b%s) value contains elements outside the universe 0b%s",
              mask, Long.toBinaryString(mask), Long.toBinaryString(mask & ~allValues)));
    }

    long oldElements = elements;
    elements = mask;
    return oldElements != elements;
  }

  @Override
  public String toString() {
    if (elements == 0) {
      return elementType.getSimpleName() + ": { }";
    }

    DelimitedBuilder out = new DelimitedBuilder(", ");

    out.append(elementType.getSimpleName()).append(": { ");
    for (E e : universe) {
      if (any(mask(e))) {
        out.append(e.name());
        if (e instanceof BitField && ((BitField) e).getBits() > 1) {
          out.append("(").append(getLong(e)).append(")");
        }
        out.mark();
      }
    }
    out.unmark().append(" }");
    return out.toString();
  }

  public boolean andNotMask(long mask) {
    long oldElements = elements;
    elements &= ~mask;
    return oldElements != elements;
  }

  public boolean any(long mask) {
    return (elements & mask) != 0;
  }

  public static <E extends Enum<E>> int intMask(E e1) {
    long[] bitMasks = getBitMasks(e1.getDeclaringClass());
    int totalBits = getTotalBits(bitMasks);
    if (totalBits > 32)
      throw new IllegalArgumentException(
          String.format("Enum fields use %d, which is more than 32 available in int", totalBits));
    return (int) bitMasks[e1.ordinal()];
  }

  private long getSigned(E e1, int maxBits, String typeName) {
    return getSignedBitField(elements, e1, maxBits, typeName);
  }

  /**
   * Returns signed value for the field, except if the field is 64 bits
   *
   * @param e1 field to get
   * @return unsigned value
   */
  private long getLong(E e1) {
    return getSigned(e1, 64, "long");
  }

  public static boolean any(long flags, long mask) {
    return (flags & mask) != 0;
  }

  public static boolean all(long flags, long mask) {
    return (flags & mask) == mask;
  }

  private long mask(E e1) {
    return bitMasks[e1.ordinal()];
  }

  /**
   * Returns an iterator over the elements contained in this set. The iterator traverses the
   * elements in their <i>natural order</i> (which is the order in which the enum constants are
   * declared). The returned Iterator is a "snapshot" iterator that will never throw {@link
   * java.util.ConcurrentModificationException}; the elements are traversed as they existed when
   * this call was invoked.
   *
   * <p>NOTE: bit field iteration requires skipping fields whose bits are all 0 so constant time is
   * violated
   *
   * @return an iterator over the elements contained in this set
   */
  @Override
  public Iterator<E> iterator() {
    return bitMasks.length == totalBits ? new EnumBitSetIterator<>() : new EnumBitFieldIterator<>();
  }

  private class EnumBitSetIterator<E extends Enum<E>> implements Iterator<E> {
    /** A bit vector representing the elements in the set not yet returned by this iterator. */
    long unseen;

    /**
     * The bit representing the last element returned by this iterator but not removed, or zero if
     * no such element exists.
     */
    long lastReturned = 0;

    EnumBitSetIterator() {
      unseen = elements;
    }

    @Override
    public boolean hasNext() {
      return unseen != 0;
    }

    @Override
    public E next() {
      if (unseen == 0) {
        throw new NoSuchElementException();
      }
      lastReturned = unseen & -unseen;
      unseen -= lastReturned;
      return (E) universe[Long.numberOfTrailingZeros(lastReturned)];
    }

    @Override
    public void remove() {
      if (lastReturned == 0) {
        throw new IllegalStateException();
      }
      elements &= ~lastReturned;
      lastReturned = 0;
    }
  }

  private class EnumBitFieldIterator<E extends Enum<E>> implements Iterator<E> {
    int nextIndex;
    int lastReturnedIndex = -1;

    EnumBitFieldIterator() {
      nextIndex = -1;
      findNext();
    }

    @Override
    public boolean hasNext() {
      return nextIndex < universe.length;
    }

    @Override
    public E next() {
      if (nextIndex >= universe.length) {
        throw new NoSuchElementException();
      }

      lastReturnedIndex = nextIndex;
      findNext();

      return (E) universe[lastReturnedIndex];
    }

    void findNext() {
      do {
        nextIndex++;
        if (nextIndex >= universe.length) {
          break;
        }
      } while ((elements & bitMasks[nextIndex]) == 0);
    }

    @Override
    public void remove() {
      if (lastReturnedIndex == -1) {
        throw new IllegalStateException();
      }
      elements &= ~bitMasks[lastReturnedIndex];
      lastReturnedIndex = -1;
    }
  }

  /**
   * Returns the number of elements in this set.
   *
   * @return the number of elements in this set
   */
  @Override
  public int size() {
    return totalBits;
  }

  /**
   * @return true if this set contains no elements
   */
  @Override
  public boolean isEmpty() {
    return elements == 0;
  }

  /**
   * Returns true if this set contains the specified element.
   *
   * @param object element to be checked for containment in this collection
   * @return true if this set contains the specified element
   */
  @Override
  public boolean contains(Object object) {
    if (object == null) {
      return false;
    }
    Class<?> eClass = object.getClass();
    if (eClass != elementType && eClass.getSuperclass() != elementType) {
      return false;
    }

    return (elements & bitMasks[((Enum<?>) object).ordinal()]) != 0;
  }

  // Modification Operations

  /**
   * Adds the specified element to this set if it is not already present.
   *
   * @param e element to be added to this set
   * @return true if the set changed as a result of the call
   * @throws NullPointerException if e is null
   */
  @Override
  public boolean add(E e) {
    typeCheck(e);

    long oldElements = elements;
    elements |= bitMasks[e.ordinal()];
    return elements != oldElements;
  }

  /**
   * Removes the specified element from this set if it is present.
   *
   * @param object element to be removed from this set, if present
   * @return true if the set contained the specified element
   */
  @Override
  public boolean remove(Object object) {
    if (object == null) {
      return false;
    }
    Class<?> eClass = object.getClass();
    if (eClass != elementType && eClass.getSuperclass() != elementType) {
      return false;
    }

    long oldElements = elements;
    elements &= ~bitMasks[((Enum<?>) object).ordinal()];
    return elements != oldElements;
  }

  // Bulk Operations

  /**
   * Returns true if this set contains all of the elements in the specified collection.
   *
   * @param c collection to be checked for containment in this set
   * @return true if this set contains all of the elements in the specified collection
   * @throws NullPointerException if the specified collection is null
   */
  @Override
  public boolean containsAll(Collection<?> c) {
    if (!(c instanceof BitFieldSet)) {
      return super.containsAll(c);
    }

    BitFieldSet<?> es = (BitFieldSet<?>) c;
    if (es.elementType != elementType) {
      return es.isEmpty();
    }

    return (es.elements & ~elements) == 0;
  }

  /**
   * Adds all of the elements in the specified collection to this set.
   *
   * @param c collection whose elements are to be added to this set
   * @return true if this set changed as a result of the call
   * @throws NullPointerException if the specified collection or any of its elements are null
   */
  @Override
  public boolean addAll(Collection<? extends E> c) {
    if (!(c instanceof BitFieldSet)) {
      return super.addAll(c);
    }

    BitFieldSet<?> es = (BitFieldSet<?>) c;
    if (es.elementType != elementType) {
      if (es.isEmpty()) {
        return false;
      }

      throw new ClassCastException(es.elementType + " != " + elementType);
    }

    long oldElements = elements;
    elements |= es.elements;
    return elements != oldElements;
  }

  /**
   * Removes from this set all of its elements that are contained in the specified collection.
   *
   * @param c elements to be removed from this set
   * @return true if this set changed as a result of the call
   * @throws NullPointerException if the specified collection is null
   */
  @Override
  public boolean removeAll(Collection<?> c) {
    if (!(c instanceof BitFieldSet)) {
      return super.removeAll(c);
    }

    BitFieldSet<?> es = (BitFieldSet<?>) c;
    if (es.elementType != elementType) {
      return false;
    }

    long oldElements = elements;
    elements &= ~es.elements;
    return elements != oldElements;
  }

  /**
   * Retains only the elements in this set that are contained in the specified collection.
   *
   * @param c elements to be retained in this set
   * @return true if this set changed as a result of the call
   * @throws NullPointerException if the specified collection is null
   */
  @Override
  public boolean retainAll(Collection<?> c) {
    if (!(c instanceof BitFieldSet)) {
      return super.retainAll(c);
    }

    BitFieldSet<?> es = (BitFieldSet<?>) c;
    if (es.elementType != elementType) {
      boolean changed = (elements != 0);
      elements = 0;
      return changed;
    }

    long oldElements = elements;
    elements &= es.elements;
    return elements != oldElements;
  }

  /** Removes all of the elements from this set. */
  @Override
  public void clear() {
    elements = 0;
  }

  /**
   * Create a bit enum set from a bit mask
   *
   * @param enumClass class of the enum
   * @param mask bit mask for items
   * @param <T> enum type
   * @return bit enum set
   */
  public static <T extends Enum<T>> BitFieldSet<T> of(Class<T> enumClass, long mask) {
    BitFieldSet<T> optionSet = BitFieldSet.noneOf(enumClass);
    optionSet.orMask(mask);
    return optionSet;
  }

  /** Throws an exception if e is not of the correct type for this enum set. */
  private final void typeCheck(E e) {
    Class<?> eClass = e.getClass();
    if (eClass != elementType && eClass.getSuperclass() != elementType)
      throw new ClassCastException(eClass + " != " + elementType);
  }

  /**
   * Compares the specified object with this set for equality. Returns true if the given object is
   * also a set, the two sets have the same size, and every member of the given set is contained in
   * this set.
   *
   * @param object object to be compared for equality with this set
   * @return true if the specified object is equal to this set
   */
  @Override
  public boolean equals(Object object) {
    if (!(object instanceof BitFieldSet)) {
      return super.equals(object);
    }

    BitFieldSet<?> es = (BitFieldSet<?>) object;
    if (es.elementType != elementType) {
      return elements == 0 && es.elements == 0;
    }
    return es.elements == elements;
  }

  /**
   * Creates an empty enum set with the specified element type.
   *
   * @param <E> The class of the elements in the set
   * @param elementType the class object of the element type for this enum set
   * @return An empty enum set of the specified type.
   * @throws NullPointerException if elementType is null
   */
  private static <E extends Enum<E>> BitFieldSet<E> noneOf(Class<E> elementType) {
    if (!elementType.isEnum()) {
      throw new ClassCastException(elementType + " not an enum");
    }

    Enum<?>[] universe = getUniverse(elementType);

    return new BitFieldSet<>(elementType, universe, getBitMasks(elementType));
  }

  /**
   * Set an unsigned value for the field
   *
   * @param e1 field
   * @param value value to set
   */
  private static <E extends Enum<E>> long setUnsigned(long elements, E e1, long value) {
    Class<E> elementType = e1.getDeclaringClass();
    long[] bitMasks = getBitMasks(elementType);
    return setUnsigned(elementType, bitMasks, elements, e1, value);
  }

  /**
   * Set an unsigned value for the field
   *
   * @param e1 field
   * @param value value to set
   */
  private static <E extends Enum<E>> long setUnsigned(
      Class<E> elementType, long[] bitMasks, long elements, E e1, long value) {
    long bitMask = bitMasks[e1.ordinal()];

    int bitCount = Long.bitCount(bitMask);
    long maxValue = 1L << bitCount;

    if (bitCount < 64) {
      if (!(value >= 0 && value < maxValue))
        throw new IllegalArgumentException(
            String.format(
                "Enum field %s.%s is %d bit%s, value range is [0, %d), cannot be set to %d",
                elementType.getSimpleName(),
                e1.name(),
                bitCount,
                bitCount > 1 ? "s" : "",
                maxValue - 1,
                value));
    }

    long shiftedValue = value << Long.numberOfTrailingZeros(bitMask);
    return elements ^ ((elements ^ shiftedValue) & bitMask);
  }

  public static <E extends Enum<E>> int setBitField(int elements, E e1, int value) {
    return (int) setUnsigned(elements, e1, value);
  }

  private static <E extends Enum<E>> long getSignedBitField(
      long elements, E e1, int maxBits, String typeName) {
    Class<E> elementType = e1.getDeclaringClass();
    long[] bitMasks = getBitMasks(elementType);
    long bitMask = bitMasks[e1.ordinal()];
    int bitCount = Long.bitCount(bitMask);

    if (bitCount > maxBits)
      throw new IllegalArgumentException(
          String.format(
              "Enum field %s.%s uses %d, which is more than %d available in %s",
              elementType.getSimpleName(), e1.name(), bitCount, maxBits, typeName));

    return elements << Long.numberOfLeadingZeros(bitMask) >> 64 - bitCount;
  }
}
