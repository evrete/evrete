package org.evrete.runtime;

import org.evrete.runtime.structure.AggregateLhsDescriptor;

public class RuntimeAggregateLhs extends RuntimeLhs {

    public RuntimeAggregateLhs(RuntimeRule rule, RuntimeLhs parent, AggregateLhsDescriptor descriptor) {
        super(rule, parent, descriptor);
    }
}
