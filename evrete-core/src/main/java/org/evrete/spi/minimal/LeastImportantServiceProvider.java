package org.evrete.spi.minimal;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RuntimeContext;

import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

abstract class LeastImportantServiceProvider implements OrderedServiceProvider {
    private static final int ORDER = Integer.MAX_VALUE;
    private final WeakHashMap<RuntimeContext<?>, Map<ProtectionDomain, JcCompiler>> javaCompilers = new WeakHashMap<>();

    final JcCompiler getCreateJavaCompiler(RuntimeContext<?> ctx, ProtectionDomain protectionDomain) {
        return javaCompilers.computeIfAbsent(ctx, k -> new HashMap<>())
                .computeIfAbsent(protectionDomain, JcCompiler::new);
    }

    @Override
    public int sortOrder() {
        return ORDER;
    }
}
