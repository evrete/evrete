package org.evrete.api.spi;

import org.evrete.api.NamedType;
import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeContext;
import org.evrete.runtime.compiler.CompilationException;

import java.util.Collection;
import java.util.function.Consumer;

//TODO remove
public interface LiteralRhsCompiler extends OrderedServiceProvider {
    default Consumer<RhsContext> compileRhs(RuntimeContext<?> context, String literalRhs, Collection<NamedType> factTypes) throws CompilationException {
        NamedType[] types = factTypes.toArray(new NamedType[0]);
        return compileRhs(context, literalRhs, types);
    }

    Consumer<RhsContext> compileRhs(RuntimeContext<?> context, String literalRhs, NamedType[] types) throws CompilationException;
}
