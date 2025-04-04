package com.vladsch.flexmark.test.util;

import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * Instance based on aggregated options used for spec test settings which may contain other such
 * settings which can be accessed through other data keys directly
 *
 * <p>For example: Rendering profile contains HTML, Parser and CSS settings. Rendering profile and
 * its contained settings can be set by spec options. In order to handle this properly rendering
 * profile settable instance is defined with HTML, Parser and CSS extracted settable instances. thus
 * allowing setting options on contained instances directly or through the rendering profile
 * container, while keeping the results consistent.
 *
 * @param <T> type for the setting
 */
public final class SettableInstance<T> {
  private final DataKey<Consumer<T>> myConsumerKey;
  private final Collection<SettableExtractedInstance<T, ?>> myExtractedInstanceSetters;

  public SettableInstance(
      DataKey<Consumer<T>> consumerKey,
      Collection<SettableExtractedInstance<T, ?>> extractedInstanceSetters) {
    myConsumerKey = consumerKey;
    myExtractedInstanceSetters =
        extractedInstanceSetters.isEmpty() ? null : extractedInstanceSetters;
  }

  public SettableInstance(DataKey<Consumer<T>> consumerKey) {
    myConsumerKey = consumerKey;
    myExtractedInstanceSetters = null;
  }

  public T setInstanceData(T instance, DataHolder dataHolder) {
    if (dataHolder != null) {
      if (dataHolder.contains(myConsumerKey)) {
        myConsumerKey.get(dataHolder).accept(instance);
      }

      if (myExtractedInstanceSetters != null) {
        for (SettableExtractedInstance<T, ?> settableExtractedInstance :
            myExtractedInstanceSetters) {
          settableExtractedInstance.aggregate(instance, dataHolder);
        }
      }
    }
    return instance;
  }

  public DataHolder aggregateActions(
      DataHolder dataHolder, DataHolder other, DataHolder overrides) {
    DataHolder results = dataHolder;

    if (other != null
        && other.contains(myConsumerKey)
        && overrides != null
        && overrides.contains(myConsumerKey)) {
      // both, need to combine
      Consumer<T> otherSetter = myConsumerKey.get(other);
      Consumer<T> overridesSetter = myConsumerKey.get(overrides);
      results = results.toMutable().set(myConsumerKey, otherSetter.andThen(overridesSetter));
    }

    if (myExtractedInstanceSetters != null) {
      for (SettableExtractedInstance<T, ?> settableExtractedInstance : myExtractedInstanceSetters) {
        results = settableExtractedInstance.aggregateActions(results, other, overrides);
      }
    }

    return results;
  }
}
