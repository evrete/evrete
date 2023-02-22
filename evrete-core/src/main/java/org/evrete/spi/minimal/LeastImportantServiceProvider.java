package org.evrete.spi.minimal;

import org.evrete.api.OrderedServiceProvider;

abstract class LeastImportantServiceProvider implements OrderedServiceProvider {
    private static final int ORDER = Integer.MAX_VALUE;
/*
    private final WeakHashMap<RuntimeContext<?>, JcCompilerOLD> javaCompilers = new WeakHashMap<>();
*/

/*
    final JcCompilerOLD getCreateJavaCompiler(RuntimeContext<?> ctx) {
        return javaCompilers.computeIfAbsent(ctx, k -> new JcCompilerOLD());
    }
*/

    @Override
    public int sortOrder() {
        return ORDER;
    }
}
