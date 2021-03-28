package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.runtime.builder.LhsBuilderImpl;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

class LhsDescriptor extends AbstractLhsDescriptor {
    LhsDescriptor(AbstractRuntime<?, ?> runtime, LhsBuilderImpl<?> root, NextIntSupplier factIdGenerator, MapFunction<NamedType, FactType> rootMapping) {
        super(runtime, root, factIdGenerator, rootMapping);
    }
}


