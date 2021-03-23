package org.evrete.api.spi;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeContext;
import org.evrete.runtime.FactType;
import org.evrete.util.compiler.CompilationException;

import java.util.Collection;
import java.util.function.Consumer;

public interface LiteralRhsCompiler extends OrderedServiceProvider {
    Consumer<RhsContext> compileRhs(RuntimeContext<?> requester, String literalRhs, Collection<FactType> factTypes, Collection<String> imports) throws CompilationException;
}
