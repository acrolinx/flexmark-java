package com.vladsch.flexmark.test.util;

import com.vladsch.flexmark.util.data.DataHolder;
import com.vladsch.flexmark.util.data.DataKey;
import com.vladsch.flexmark.util.data.DataKeyAggregator;
import com.vladsch.flexmark.util.data.DataSet;
import com.vladsch.flexmark.util.data.SharedDataKeys;
import com.vladsch.flexmark.util.misc.Extension;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class LoadUnloadDataKeyAggregator implements DataKeyAggregator {
  public static final DataKey<Collection<Class<? extends Extension>>> UNLOAD_EXTENSIONS =
      new DataKey<>("UNLOAD_EXTENSIONS", Collections.emptyList());
  public static final DataKey<Collection<Extension>> LOAD_EXTENSIONS =
      new DataKey<>("LOAD_EXTENSIONS", Extension.EMPTY_LIST);
  private static final LoadUnloadDataKeyAggregator INSTANCE = new LoadUnloadDataKeyAggregator();

  static {
    DataSet.registerDataKeyAggregator(INSTANCE);
  }

  private LoadUnloadDataKeyAggregator() {}

  @Override
  public DataHolder aggregate(DataHolder combined) {
    if (combined.contains(LOAD_EXTENSIONS) || combined.contains(UNLOAD_EXTENSIONS)) {
      // have something to work with, or at least clean
      if (combined.contains(SharedDataKeys.EXTENSIONS) || combined.contains(LOAD_EXTENSIONS)) {
        Collection<Extension> extensions = SharedDataKeys.EXTENSIONS.get(combined);
        Collection<Extension> loadExtensions = LOAD_EXTENSIONS.get(combined);
        Collection<Class<? extends Extension>> unloadExtensions = UNLOAD_EXTENSIONS.get(combined);

        if (!loadExtensions.isEmpty() || !unloadExtensions.isEmpty() && !extensions.isEmpty()) {
          LinkedHashSet<Extension> resolvedExtensions = new LinkedHashSet<>(extensions);
          resolvedExtensions.addAll(loadExtensions);
          resolvedExtensions.removeIf(extension -> unloadExtensions.contains(extension.getClass()));
          return combined
              .toMutable()
              .remove(LOAD_EXTENSIONS)
              .remove(UNLOAD_EXTENSIONS)
              .set(SharedDataKeys.EXTENSIONS, new ArrayList<>(resolvedExtensions))
              .toImmutable();
        }
      }
      return combined.toMutable().remove(LOAD_EXTENSIONS).remove(UNLOAD_EXTENSIONS);
    }
    return combined;
  }

  @Override
  public DataHolder aggregateActions(DataHolder combined, DataHolder other, DataHolder overrides) {
    if (other.contains(LOAD_EXTENSIONS) && overrides.contains(LOAD_EXTENSIONS)) {
      // have to combine these
      List<Extension> loadExtensions = new ArrayList<>(LOAD_EXTENSIONS.get(other));
      loadExtensions.addAll(LOAD_EXTENSIONS.get(overrides));
      combined = combined.toMutable().set(LOAD_EXTENSIONS, loadExtensions);
    }

    if (other.contains(UNLOAD_EXTENSIONS) && overrides.contains(UNLOAD_EXTENSIONS)) {
      // have to combine these
      List<Class<? extends Extension>> loadExtensions =
          new ArrayList<>(UNLOAD_EXTENSIONS.get(other));
      loadExtensions.addAll(UNLOAD_EXTENSIONS.get(overrides));
      combined = combined.toMutable().set(UNLOAD_EXTENSIONS, loadExtensions);
    }
    return combined;
  }

  @Override
  public DataHolder clean(DataHolder combined) {
    if (combined.contains(LOAD_EXTENSIONS) || combined.contains(UNLOAD_EXTENSIONS)) {
      return combined.toMutable().remove(LOAD_EXTENSIONS).remove(UNLOAD_EXTENSIONS);
    }
    return combined;
  }

  @Override
  public Set<Class<?>> invokeAfterSet() {
    return null;
  }
}
