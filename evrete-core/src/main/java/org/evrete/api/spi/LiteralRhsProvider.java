package org.evrete.api.spi;

import org.evrete.api.OrderedServiceProvider;
import org.evrete.api.RhsContext;
import org.evrete.api.RuntimeContext;
import org.evrete.runtime.FactType;

import java.util.Collection;
import java.util.function.Consumer;

public interface LiteralRhsProvider extends OrderedServiceProvider {
    Consumer<RhsContext> buildRhs(RuntimeContext<?> requester, String literalRhs, Collection<FactType> factTypes, Collection<String> imports);
}
