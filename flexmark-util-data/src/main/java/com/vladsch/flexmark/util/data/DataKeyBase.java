package com.vladsch.flexmark.util.data;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DataKeyBase<T> implements MutableDataValueSetter<T> {
  private final @NotNull String name;
  private final @NotNull DataValueFactory<T> factory;
  private final T defaultValue;

  /**
   * Creates a NullableDataKey with a computed default value and a provided default value when data
   * holder is null.
   *
   * <p>Use this constructor to ensure that factory is never called with null data holder value
   *
   * @param name See {@link #getName()}.
   * @param defaultValue default to use when data holder is null
   * @param factory data value factory for creating a new default value for the key for a non-null
   *     data holder
   */
  DataKeyBase(@NotNull String name, T defaultValue, @NotNull DataValueFactory<T> factory) {
    this.name = name;
    this.defaultValue = defaultValue;
    this.factory = factory;
  }

  @NotNull
  public String getName() {
    return name;
  }

  @NotNull
  public DataValueFactory<T> getFactory() {
    return factory;
  }

  public T getDefaultValue() {
    return defaultValue;
  }

  public T getDefaultValue(@NotNull DataHolder holder) {
    return factory.apply(holder);
  }

  public T get(@Nullable DataHolder holder) {
    return holder == null ? defaultValue : (T) holder.getOrCompute(this, this::getDefaultValue);
  }

  @Override
  public String toString() {
    if (defaultValue != null) {
      return "NullableDataKey<" + defaultValue.getClass().getSimpleName() + "> " + name;
    }

    return "NullableDataKey<unknown> " + name;
  }

  /**
   * Compare only by address. Every key instance is unique
   *
   * @param object other
   * @return true if equal
   */
  @Override
  public final boolean equals(Object object) {
    return this == object;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }
}
