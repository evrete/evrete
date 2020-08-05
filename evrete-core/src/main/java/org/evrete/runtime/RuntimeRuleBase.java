package org.evrete.runtime;

import org.evrete.api.ValueRow;
import org.evrete.collections.FastHashSet;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.structure.FactType;

import java.util.function.Function;

/**
 * A rule-wide data structure
 */
public abstract class RuntimeRuleBase {
    private final RuntimeFactType[] factSources;
    private final SessionMemory memory;
    private final Function<FactType, FastHashSet<ValueRow>> deletedKeys;

    private boolean insertDeltaAvailable = false;
    private boolean deleteDeltaAvailable = false;

    protected RuntimeRuleBase(FactType[] allFactTypes, SessionMemory memory) {
        this.memory = memory;
        this.factSources = buildTypes(memory, allFactTypes);
        this.deletedKeys = factType -> factSources[factType.getInRuleIndex()].getDeleteTasks();
    }

    public Function<FactType, FastHashSet<ValueRow>> getDeletedKeys() {
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

    public void markInsertDeltaAvailable() {
        this.insertDeltaAvailable = true;
    }

    public boolean isInsertDeltaAvailable() {
        return insertDeltaAvailable;
    }

    public void markDeleteDeltaAvailable() {
        this.deleteDeltaAvailable = true;
    }

    public boolean isDeleteDeltaAvailable() {
        return deleteDeltaAvailable;
    }

    public void resetDeltaState() {
        this.deleteDeltaAvailable = false;
        this.insertDeltaAvailable = false;
        for (RuntimeFactType factType : factSources) {
            factType.resetDeleteDeltaAvailable();
            factType.resetInsertDeltaAvailable();
            ValuesRowSet delTasks = factType.getDeleteTasks();
            if (delTasks != null) {
                delTasks.clear();
            }
        }
    }

    public SessionMemory getMemory() {
        return memory;
    }

    static class ValuesRowSet extends FastHashSet<ValueRow> {
    }
}
