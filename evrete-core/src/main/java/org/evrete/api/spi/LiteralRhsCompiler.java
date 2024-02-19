package org.evrete.api.spi;

import org.evrete.api.NamedType;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeContext;
import org.evrete.runtime.compiler.CompilationException;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * @deprecated use {@link LiteralSourceCompiler} instead
 */
@Deprecated
public interface LiteralRhsCompiler extends OrderedServiceProvider {
    default Consumer<RhsContext> compileRhs(RuntimeContext<?> context, String literalRhs, Collection<NamedType> factTypes) throws CompilationException {
        throw new UnsupportedOperationException("Deprecated");
    }

    default Consumer<RhsContext> compileRhs(RuntimeContext<?> context, String literalRhs, NamedType[] types) throws CompilationException {
        throw new UnsupportedOperationException("Deprecated");
    }
}
