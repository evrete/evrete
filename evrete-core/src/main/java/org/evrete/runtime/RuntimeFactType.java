package org.evrete.runtime;

import org.evrete.api.ReIterable;
import org.evrete.api.RuntimeFact;
import org.evrete.api.ValueRow;
import org.evrete.runtime.memory.SessionMemory;
import org.evrete.runtime.structure.FactType;

public abstract class RuntimeFactType extends FactType implements MemoryChangeListener, ReIterable<RuntimeFact> {
    public static final RuntimeFactType[] ZERO_ARRAY = new RuntimeFactType[0];
    private boolean insertDeltaAvailable;
    private boolean deleteDeltaAvailable;
    private final SessionMemory runtime;
    private RuntimeRule rule;
    private final RuntimeRuleBase.ValuesRowSet deleteTasks;

    RuntimeFactType(SessionMemory runtime, FactType other) {
        super(other);
        this.runtime = runtime;
        this.deleteTasks = new RuntimeRuleBase.ValuesRowSet();
    }

    public static RuntimeFactType factory(FactType type, SessionMemory runtime) {
        if (type.getFields().size() > 0) {
            return new RuntimeFactTypeKeyed(runtime, type);
        } else {
            return new RuntimeFactTypePlain(runtime, type);
        }
    }

    public SessionMemory getRuntime() {
        return runtime;
    }

    public RuntimeRule getRule() {
        return rule;
    }

    public void addToDeleteKey(ValueRow key) {
        deleteTasks.add(key);
        markDeleteDeltaAvailable();
    }

    public void setRule(RuntimeRule rule) {
        this.rule = rule;
    }

    /*
    public SessionMemory getMemory() {
        return rule.getMemory();
    }
*/

    RuntimeRuleBase.ValuesRowSet getDeleteTasks() {
        return deleteTasks;
    }

    public boolean isInsertDeltaAvailable() {
        return insertDeltaAvailable;
    }

    public void markInsertDeltaAvailable() {
        if (!this.insertDeltaAvailable) {
            this.insertDeltaAvailable = true;
            rule.markInsertDeltaAvailable();
        }
    }

    public void markDeleteDeltaAvailable() {
        if (!this.deleteDeltaAvailable) {
            this.deleteDeltaAvailable = true;
            rule.markDeleteDeltaAvailable();
        }
    }

    public void resetInsertDeltaAvailable() {
        this.insertDeltaAvailable = false;
    }

    public void resetDeleteDeltaAvailable() {
        this.deleteDeltaAvailable = false;
    }

    public boolean isDeleteDeltaAvailable() {
        return deleteDeltaAvailable;
    }
}
