package com.vladsch.flexmark.html;

import java.util.Set;
import org.jetbrains.annotations.Nullable;

public abstract class IndependentLinkResolverFactory implements LinkResolverFactory {
  @Nullable
  @Override
  public Set<Class<?>> getAfterDependents() {
    return null;
  }

  @Nullable
  @Override
  public Set<Class<?>> getBeforeDependents() {
    return null;
  }

  @Override
  public boolean affectsGlobalScope() {
    return false;
  }
}
