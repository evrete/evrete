package org.evrete.spi.minimal;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;

import java.util.WeakHashMap;

abstract class LeastImportantServiceProvider implements OrderedServiceProvider {
    private static final int ORDER = Integer.MAX_VALUE;
    private final WeakHashMap<RuntimeContext<?>, JcCompiler> javaCompilers = new WeakHashMap<>();

    final JcCompiler getCreateJavaCompiler(RuntimeContext<?> ctx) {
        return javaCompilers.computeIfAbsent(ctx, k -> new JcCompiler());
    }

    @Override
    public int sortOrder() {
        return ORDER;
    }
}
