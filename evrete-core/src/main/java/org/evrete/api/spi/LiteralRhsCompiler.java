package org.evrete.api.spi;

import org.evrete.api.NamedType;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeContext;
import org.evrete.util.compiler.CompilationException;

import java.util.Collection;
import java.util.function.Consumer;

public interface LiteralRhsCompiler extends OrderedServiceProvider {
    default Consumer<RhsContext> compileRhs(RuntimeContext<?> requester, String literalRhs, Collection<NamedType> factTypes) throws CompilationException {
        NamedType[] types = factTypes.toArray(new NamedType[0]);
        return compileRhs(requester, literalRhs, types);
    }

    Consumer<RhsContext> compileRhs(RuntimeContext<?> requester, String literalRhs, NamedType[] types) throws CompilationException;
}
