package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.runtime.builder.LhsBuilder;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

class LhsDescriptor extends AbstractLhsDescriptor {
    LhsDescriptor(AbstractRuntime<?, ?> runtime, LhsBuilder<?> root, NextIntSupplier factIdGenerator, MapFunction<NamedType, FactType> rootMapping) {
        super(runtime, root, factIdGenerator, rootMapping);
    }
}


