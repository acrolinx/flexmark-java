package com.vladsch.flexmark.html;

import com.vladsch.flexmark.html.renderer.LinkResolverBasicContext;
import com.vladsch.flexmark.util.dependency.Dependent;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface LinkResolverFactory
    extends Function<LinkResolverBasicContext, LinkResolver>, Dependent {
  @Override
  @Nullable
  Set<Class<?>> getAfterDependents();

  @Override
  @Nullable
  Set<Class<?>> getBeforeDependents();

  @Override
  boolean affectsGlobalScope();

  @Override
  @NotNull
  LinkResolver apply(@NotNull LinkResolverBasicContext context);
}
