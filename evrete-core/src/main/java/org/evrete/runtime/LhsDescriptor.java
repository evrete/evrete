package org.evrete.runtime;

import org.evrete.api.NamedType;
import org.evrete.runtime.builder.LhsBuilder;
import org.evrete.util.MapFunction;
import org.evrete.util.NextIntSupplier;

public class LhsDescriptor extends AbstractLhsDescriptor {
    LhsDescriptor(AbstractRuntime<?> runtime, LhsBuilder<?> root, NextIntSupplier factIdGenerator, MapFunction<NamedType, FactType> rootMapping) {
        super(runtime, null, root, factIdGenerator, rootMapping);
    }
}


