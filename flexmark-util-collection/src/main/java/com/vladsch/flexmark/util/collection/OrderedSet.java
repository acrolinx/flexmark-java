package com.vladsch.flexmark.util.collection;

import com.vladsch.flexmark.util.collection.iteration.BitSetIterable;
import com.vladsch.flexmark.util.collection.iteration.BitSetIterator;
import com.vladsch.flexmark.util.collection.iteration.Indexed;
import com.vladsch.flexmark.util.collection.iteration.IndexedIterable;
import com.vladsch.flexmark.util.collection.iteration.IndexedIterator;
import com.vladsch.flexmark.util.collection.iteration.ReversibleIndexedIterator;
import com.vladsch.flexmark.util.collection.iteration.ReversibleIterable;
import com.vladsch.flexmark.util.collection.iteration.ReversibleIterator;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class OrderedSet<E> implements Set<E> {
  private final HashMap<E, Integer> keyMap;
  private final ArrayList<E> valueList;
  private final CollectionHost<E> host;
  private Indexed<E> indexedProxy;
  private Indexed<E> allowConcurrentModsIndexedProxy;
  private final BitSet validIndices;
  private int modificationCount;

  public OrderedSet() {
    this(0);
  }

  private OrderedSet(int capacity) {
    this(capacity, null);
  }

  OrderedSet(int capacity, CollectionHost<E> host) {
    this.keyMap = new HashMap<>(capacity);
    this.valueList = new ArrayList<>(capacity);
    this.validIndices = new BitSet();
    this.host = host;
    this.modificationCount = Integer.MIN_VALUE;
    this.indexedProxy = null;
    this.allowConcurrentModsIndexedProxy = null;
  }

  public BitSet indexBitSet(Iterable<? extends E> items) {
    BitSet bitSet = new BitSet();
    for (E element : items) {
      int i = indexOf(element);
      if (i != -1) {
        bitSet.set(i);
      }
    }
    return bitSet;
  }

  public BitSet keyDifferenceBitSet(Iterable<? extends Map.Entry<? extends E, ?>> items) {
    return keyDifferenceBitSet(items.iterator());
  }

  private BitSet keyDifferenceBitSet(Iterator<? extends Map.Entry<? extends E, ?>> items) {
    BitSet bitSet = new BitSet();
    int j = 0;
    while (items.hasNext()) {
      Map.Entry<? extends E, ?> entry = items.next();
      int i = indexOf(entry.getKey());
      if (i != j) {
        bitSet.set(i);
      }
      j++;
    }
    return bitSet;
  }

  private class IndexedProxy implements Indexed<E> {
    private final boolean allowConcurrentMods;

    IndexedProxy(boolean allowConcurrentMods) {
      this.allowConcurrentMods = allowConcurrentMods;
    }

    @Override
    public E get(int index) {
      return getValue(index);
    }

    @Override
    public void set(int index, E item) {
      setValueAt(index, item, null);
    }

    @Override
    public void removeAt(int index) {
      removeIndexHosted(index);
    }

    @Override
    public int size() {
      return valueList.size();
    }

    @Override
    public int modificationCount() {
      return allowConcurrentMods ? 0 : getIteratorModificationCount();
    }
  }

  public Indexed<E> getIndexedProxy() {
    if (indexedProxy != null) {
      return indexedProxy;
    }
    indexedProxy = new IndexedProxy(false);
    return indexedProxy;
  }

  public Indexed<E> getConcurrentModsIndexedProxy() {
    if (allowConcurrentModsIndexedProxy != null) {
      return allowConcurrentModsIndexedProxy;
    }
    allowConcurrentModsIndexedProxy = new IndexedProxy(true);
    return allowConcurrentModsIndexedProxy;
  }

  public int getModificationCount() {
    return modificationCount;
  }

  private int getIteratorModificationCount() {
    return host != null ? host.getIteratorModificationCount() : modificationCount;
  }

  private static <T1> T1 ifNull(T1 o, T1 nullValue) {
    return o == null ? nullValue : o;
  }

  public int indexOf(Object element) {
    return ifNull(keyMap.get(element), -1);
  }

  boolean isValidIndex(int index) {
    return index >= 0 && index < valueList.size() && validIndices.get(index);
  }

  private void validateIndex(int index) {
    if (!isValidIndex(index)) {
      throw new IndexOutOfBoundsException(
          "Index "
              + index
              + " is not valid, size="
              + valueList.size()
              + " validIndices["
              + index
              + "]="
              + validIndices.get(index));
    }
  }

  E getValue(int index) {
    validateIndex(index);
    return valueList.get(index);
  }

  E getValueOrNull(int index) {
    return isValidIndex(index) ? valueList.get(index) : null;
  }

  @Override
  public int size() {
    return keyMap.size();
  }

  @Override
  public boolean isEmpty() {
    return keyMap.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return keyMap.containsKey(o);
  }

  public List<E> getValueList() {
    return valueList;
  }

  boolean setValueAt(int index, E value, Object o) {
    // if index is after end then we add nulls
    int indexOf = indexOf(value);
    if (indexOf != -1) {
      if (index != indexOf) {
        throw new IllegalStateException(
            "Trying to add existing element " + value + "[" + indexOf + "] at index " + index);
      }
      // same index, same element, nothing to update
      return false;
    }

    if (index < valueList.size()) {
      if (validIndices.get(index)) {
        // already have another element at index
        throw new IllegalStateException(
            "Trying to add new element "
                + value
                + " at index "
                + index
                + ", already occupied by "
                + valueList.get(index));
      }
      // old element was removed, just replace
    } else {
      if (index > valueList.size()) addNulls(index - 1);
    }

    if (host != null && !host.skipHostUpdate()) {
      host.adding(index, value, o);
    }

    keyMap.put(value, index);
    valueList.set(index, value);
    validIndices.set(index);

    return true;
  }

  public boolean isSparse() {
    return validIndices.nextClearBit(0) < valueList.size();
  }

  void addNull() {
    addNulls(valueList.size());
  }

  void addNulls(int index) {
    if (host != null && !host.skipHostUpdate()) {
      host.addingNulls(index);
    }

    ++modificationCount;

    // no need they are 0's by default
    while (valueList.size() <= index) valueList.add(null);
  }

  ReversibleIterator<Integer> indexIterator() {
    return new BitSetIterator(validIndices);
  }

  ReversibleIterator<Integer> reversedIndexIterator() {
    return new BitSetIterator(validIndices, true);
  }

  ReversibleIterable<Integer> indexIterable() {
    return new BitSetIterable(validIndices);
  }

  ReversibleIterable<Integer> reversedIndexIterable() {
    return new BitSetIterable(validIndices, true);
  }

  @Override
  public ReversibleIndexedIterator<E> iterator() {
    return new IndexedIterator<>(getIndexedProxy(), indexIterator());
  }

  ReversibleIndexedIterator<E> reversedIterator() {
    return new IndexedIterator<>(getIndexedProxy(), reversedIndexIterator());
  }

  private ReversibleIterable<E> iterable() {
    return new IndexedIterable<>(getIndexedProxy(), indexIterable());
  }

  ReversibleIterable<E> reversedIterable() {
    return new IndexedIterable<>(getIndexedProxy(), reversedIndexIterable());
  }

  @Override
  public Object[] toArray() {
    Object[] objects = new Object[keyMap.size()];
    int index = -1;
    int i = -1;
    while (index + 1 < valueList.size()) {
      if (!validIndices.get(++index)) {
        continue;
      }
      objects[++i] = valueList.get(index);
    }
    return objects;
  }

  @Override
  public <T> T[] toArray(T[] array) {
    Object[] objects = array;

    if (array.length < keyMap.size()) {
      objects =
          array.getClass() == Object[].class
              ? new Object[keyMap.size()]
              : (Object[]) Array.newInstance(array.getClass().getComponentType(), keyMap.size());
    }

    int index = -1;
    int i = -1;
    while (index + 1 < valueList.size()) {
      if (!validIndices.get(++index)) {
        continue;
      }
      objects[++i] = valueList.get(index);
    }

    if (objects.length > ++i) {
      objects[i] = null;
    }
    return (T[]) objects;
  }

  @Override
  public boolean add(E e) {
    return add(e, null);
  }

  boolean add(E e, Object o) {
    if (keyMap.containsKey(e)) {
      return false;
    }

    int i = valueList.size();

    if (host != null && !host.skipHostUpdate()) {
      host.adding(i, e, o);
    }

    ++modificationCount;
    keyMap.put(e, i);
    valueList.add(e);
    validIndices.set(i);
    return true;
  }

  boolean removeIndex(int index) {
    return this.removeIndexHosted(index) != null;
  }

  Object removeIndexHosted(int index) {
    validateIndex(index);

    E o = valueList.get(index);
    Object r = null;
    if (host != null && !host.skipHostUpdate()) {
      r = host.removing(index, o);
    } else {
      r = o;
    }

    ++modificationCount;
    keyMap.remove(o);

    if (keyMap.size() == 0) {
      if (host != null && !host.skipHostUpdate()) {
        host.clearing();
      }
      valueList.clear();
      validIndices.clear();
    } else {
      if (host == null && index == valueList.size() - 1) {
        valueList.remove(index);
      }
      validIndices.clear(index);
    }

    return r;
  }

  @Override
  public boolean remove(Object o) {
    return removeHosted(o) != null;
  }

  Object removeHosted(Object o) {
    Integer index = keyMap.get(o);
    if (index == null) {
      return null;
    }
    return removeIndexHosted(index);
  }

  @Override
  public boolean containsAll(Collection<?> collection) {
    for (Object o : collection) {
      if (!keyMap.containsKey(o)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public boolean addAll(Collection<? extends E> collection) {
    boolean[] changed = {false};
    for (E e : collection) {
      if (add(e)) changed[0] = true;
    }
    return changed[0];
  }

  @Override
  public boolean retainAll(Collection<?> collection) {
    BitSet removeSet = new BitSet(valueList.size());
    removeSet.set(0, valueList.size());
    removeSet.and(validIndices);

    for (Object o : collection) {
      int index = indexOf(o);
      if (index != -1) {
        removeSet.clear(index);
      }
    }

    // Java7
    int index = valueList.size();
    if (index == 0) {
      return false;
    }
    boolean changed = false;
    while (index-- > 0) {
      index = removeSet.previousSetBit(index);
      if (index == -1) {
        break;
      }
      remove(valueList.get(index));
      changed = true;
    }

    return changed;
  }

  @Override
  public boolean removeAll(Collection<?> collection) {
    boolean changed = false;
    for (Object o : collection) {
      if (keyMap.containsKey(o)) {
        if (remove(o)) changed = true;
      }
    }
    return changed;
  }

  @Override
  public void clear() {
    if (host != null && !host.skipHostUpdate()) {
      host.clearing();
    }

    ++modificationCount;
    keyMap.clear();
    valueList.clear();
    validIndices.clear();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }

    OrderedSet<?> set = (OrderedSet<?>) object;

    if (size() != set.size()) {
      return false;
    }
    Iterator<?> setIterator = set.iterator();
    for (Object e : this) {
      Object eSet = setIterator.next();
      if (!e.equals(eSet)) {
        return false;
      }
    }
    return true;
  }

  @Override
  public int hashCode() {
    int result = keyMap.hashCode();
    result = 31 * result + valueList.hashCode();
    result = 31 * result + validIndices.hashCode();
    return result;
  }

  public BitSet getValidIndices() {
    return validIndices;
  }
}
