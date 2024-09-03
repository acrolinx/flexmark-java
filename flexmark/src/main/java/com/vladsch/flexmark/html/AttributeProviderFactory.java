package com.vladsch.flexmark.html;

import com.vladsch.flexmark.html.renderer.LinkResolverContext;
import com.vladsch.flexmark.util.dependency.Dependent;
import java.util.Set;
import java.util.function.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface AttributeProviderFactory extends Function<LinkResolverContext, AttributeProvider>, Dependent {
    @Override
    @Nullable Set<Class<?>> getAfterDependents();

    @Override
    @Nullable Set<Class<?>> getBeforeDependents();

    @Override
    boolean affectsGlobalScope();

    @Override
    @NotNull AttributeProvider apply(@NotNull LinkResolverContext context);
}
