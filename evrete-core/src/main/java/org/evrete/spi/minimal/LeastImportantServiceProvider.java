package org.evrete.spi.minimal;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;

import java.util.WeakHashMap;

abstract class LeastImportantServiceProvider implements OrderedServiceProvider {
    private static final int ORDER = Integer.MAX_VALUE;
    private final WeakHashMap<RuntimeContext<?>, JcClassLoader> classLoaders = new WeakHashMap<>();

    final JcClassLoader getCreateClassLoader(RuntimeContext<?> ctx) {
        return classLoaders.computeIfAbsent(ctx, k -> new JcClassLoader(ctx.getClassLoader()));
    }

    @Override
    public int sortOrder() {
        return ORDER;
    }
}
