package org.evrete.runtime;

import org.evrete.api.ValueRow;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.structure.FactType;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A rule-wide data structure
 */
public abstract class RuntimeRuleBase {
    private final RuntimeFactType[] factSources;
    private final RuntimeFactTypeKeyed[] betaFactSources;
    private final SessionMemory memory;
    private final Function<FactType, Predicate<ValueRow>> deletedKeys;

    protected RuntimeRuleBase(FactType[] allFactTypes, SessionMemory memory) {
        this.memory = memory;
        this.factSources = buildTypes(memory, allFactTypes);
        this.deletedKeys = factType -> {
            RuntimeFactTypeKeyed t = (RuntimeFactTypeKeyed) factSources[factType.getInRuleIndex()];
            return valueRow -> t.getKeyStorage().isKeyDeleted(valueRow);
        };

        List<RuntimeFactTypeKeyed> betaNodes = new ArrayList<>(factSources.length);
        for (RuntimeFactType t : factSources) {
            if (t.isBetaNode()) {
                betaNodes.add((RuntimeFactTypeKeyed) t);
            }
        }
        this.betaFactSources = betaNodes.toArray(new RuntimeFactTypeKeyed[0]);
    }

    public RuntimeFactTypeKeyed[] getBetaFactSources() {
        return betaFactSources;
    }

    public Function<FactType, Predicate<ValueRow>> getDeletedKeys() {
        return deletedKeys;
    }

    private static RuntimeFactType[] buildTypes(SessionMemory runtime, FactType[] allFactTypes) {
        RuntimeFactType[] factSources = new RuntimeFactType[allFactTypes.length];
        for (FactType factType : allFactTypes) {
            RuntimeFactType iterable = RuntimeFactType.factory(factType, runtime);
            factSources[iterable.getInRuleIndex()] = iterable;
        }
        return factSources;
    }

    @SuppressWarnings("unchecked")
    public <T extends RuntimeFactType> T resolve(FactType type) {
        return (T) this.factSources[type.getInRuleIndex()];
    }

    public RuntimeFactType[] resolve(FactType[] types) {
        RuntimeFactType[] resolved = new RuntimeFactType[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = resolve(types[i]);
        }
        return resolved;
    }

    public RuntimeFactType[] getAllFactTypes() {
        return this.factSources;
    }

/*
    public void markInsertDeltaAvailable() {
        this.insertDeltaAvailable = true;
    }

    public boolean isInsertDeltaAvailable() {
        return insertDeltaAvailable;
    }
*/

    //TODO !!! optimize
    public boolean isInsertDeltaAvailable() {
        boolean delta = false;
        for (RuntimeFactTypeKeyed ft : this.betaFactSources) {
            if (ft.isInsertDeltaAvailable()) {
                delta = true;
            }
        }
        return delta;
    }

    //TODO !!! optimize
    public boolean isDeleteDeltaAvailable() {
        boolean delta = false;
        for (RuntimeFactTypeKeyed ft : this.betaFactSources) {
            if (ft.isDeleteDeltaAvailable()) {
                delta = true;
            }
        }
        return delta;
    }

    public SessionMemory getMemory() {
        return memory;
    }
}
