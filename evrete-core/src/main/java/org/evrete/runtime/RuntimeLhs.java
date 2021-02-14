package org.evrete.runtime;

import org.evrete.api.RhsContext;
import org.evrete.api.WorkingMemory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class RuntimeLhs extends AbstractRuntimeLhs implements RhsContext {
    private final Collection<BetaEndNode> allBetaEndNodes = new ArrayList<>();
    private final Function<String, int[]> name2indices;
    private final RuntimeRuleImpl rule;
    private final WorkingMemory workingMemory;

    RuntimeLhs(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        super(rule, descriptor);
        this.name2indices = descriptor.getNameIndices();
        this.allBetaEndNodes.addAll(getEndNodes());
        this.rule = rule;
        this.workingMemory = rule.getRuntime();
    }

    static RuntimeLhs factory(RuntimeRuleImpl rule, LhsDescriptor descriptor) {
        return new RuntimeLhsDefault(rule, descriptor);
    }

    @Override
    public RuntimeRuleImpl getRule() {
        return rule;
    }

    abstract void forEach(Consumer<RhsContext> rhs);

    public final Collection<BetaEndNode> getAllBetaEndNodes() {
        return allBetaEndNodes;
    }

    @Override
    public final Object getObject(String name) {
        int[] arr = name2indices.apply(name);
        if (arr == null) throw new IllegalArgumentException("Unknown type reference: " + name);
        return factState[arr[0]][arr[1]].getFact();
    }

    @Override
    //TODO check if field values have _really_ changed
    public final RhsContext update(Object obj) {
        Objects.requireNonNull(obj);
        for (FactIterationState[] arr : factState) {
            for (FactIterationState state : arr) {
                if (state.value == obj) {
                    workingMemory.update(state.handle, state.value);
                    return this;
                }
            }
        }
        throw new IllegalArgumentException("Fact " + obj + " not found in current RHS context");
    }

    @Override
    public final RhsContext delete(Object obj) {
        Objects.requireNonNull(obj);
        for (FactIterationState[] arr : factState) {
            for (FactIterationState state : arr) {
                if (state.value == obj) {
                    //state.typeMemory.bufferDelete(state.handle);
                    workingMemory.delete(state.handle);
                    return this;
                }
            }
        }
        throw new IllegalArgumentException("Fact " + obj + " not found in current RHS context");
    }

    @Override
    public final RhsContext insert(Object obj) {
        workingMemory.insert(obj);
        return this;
    }
}
