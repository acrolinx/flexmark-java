package com.vladsch.flexmark.util.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface MutableDataHolder extends DataHolder {
  @Override
  Object getOrCompute(@NotNull DataKeyBase<?> key, @NotNull DataValueFactory<?> factory);

  /**
   * Store the given value for the key
   *
   * @param <T> data type of the data referred by the key
   * @param key data key
   * @param value value to store
   * @return mutable data holder for chained calls
   */
  @NotNull
  <T> MutableDataHolder set(@NotNull DataKey<T> key, @NotNull T value);

  /**
   * Store the given value for the key
   *
   * @param <T> data type of the data referred by the key
   * @param key data key
   * @param value value to store
   * @return mutable data holder for chained calls
   */
  @NotNull
  <T> MutableDataHolder set(@NotNull NullableDataKey<T> key, @Nullable T value);

  /**
   * Remove the stored value for the key, used to force to default or to force recompute
   *
   * @param key data key to remove
   * @return mutable data holder for chained calls
   */
  @NotNull
  MutableDataHolder remove(@NotNull DataKeyBase<?> key);

  /**
   * Store the given value for the key
   *
   * @param dataSetter data setter which will set values
   * @return mutable data holder for chained calls
   */
  @NotNull
  MutableDataHolder setFrom(@NotNull MutableDataSetter dataSetter);

  /**
   * Copy all values from one data holder to this data holder
   *
   * @param other data holder from which to copy all values
   * @return mutable data holder for chained calls
   */
  @NotNull
  MutableDataHolder setAll(@NotNull DataHolder other);

  /**
   * Set options in given mutable data holder
   *
   * @param dataHolder data holder where to copy options from this data holder
   * @return dataHolder
   */
  @NotNull
  @Override
  MutableDataHolder setIn(@NotNull MutableDataHolder dataHolder);

  /**
   * clear all options out of the data set
   *
   * @return mutable data holder for chained calls
   */
  @NotNull
  MutableDataHolder clear();
}
